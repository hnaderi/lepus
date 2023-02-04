/*
 * Copyright 2021 Hossein Naderi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lepus.client
package internal

import cats.effect.IO
import lepus.codecs.AllClassesDataGenerator
import lepus.codecs.ChannelDataGenerator
import lepus.codecs.FrameGenerators
import lepus.protocol.ChannelClass
import lepus.protocol.constants.ReplyCode
import org.scalacheck.Gen

class LowLevelChannelSuite extends InternalTestSuite {
  test("Initial status is Active") {
    for {
      ctx <- LowLevelChannelContext()
      _ <- ctx.channel.status.get.assertEquals(Channel.Status.Active)
    } yield ()
  }

  test("Handles flow control from server") {
    for {
      ctx <- LowLevelChannelContext()
      _ <- ctx.channel.method(ChannelClass.Flow(false))
      _ <- ctx.channel.status.get.assertEquals(Channel.Status.InActive)
      _ <- ctx.rpc.interactions.assert(
        FakeRPCChannel.Interaction.SendNoWait(ChannelClass.FlowOk(false))
      )
      _ <- ctx.rpc.interactions.reset
      _ <- ctx.channel.method(ChannelClass.Flow(true))
      _ <- ctx.channel.status.get.assertEquals(Channel.Status.Active)
    } yield ()
  }

  test("Handles flow control from server") {
    for {
      ctx <- LowLevelChannelContext()
      _ <- ctx.channel.method(ChannelClass.Flow(false))
      _ <- ctx.channel.status.get.assertEquals(Channel.Status.InActive)
      _ <- ctx.rpc.interactions.assert(
        FakeRPCChannel.Interaction.SendNoWait(ChannelClass.FlowOk(false))
      )
      _ <- ctx.rpc.interactions.reset
      _ <- ctx.channel.method(ChannelClass.Flow(true))
      _ <- ctx.channel.status.get.assertEquals(Channel.Status.Active)
    } yield ()
  }

  test("Must close channel where receives close method") {
    val methods = ChannelDataGenerator.closeGen

    forAllF(methods) { method =>
      for {
        ctx <- LowLevelChannelContext()
        _ <- ctx.channel.method(method)
        _ <- ctx.channel.status.get.assertEquals(Channel.Status.Closed)
        _ <- ctx.rpc.interactions.assert(
          FakeRPCChannel.Interaction.SendNoWait(ChannelClass.CloseOk)
        )
      } yield ()
    }
  }

  test("Must close channel where receives close ok method (just in case!)") {
    val methods = ChannelDataGenerator.closeOkGen

    forAllF(methods) { method =>
      for {
        ctx <- LowLevelChannelContext()
        _ <- ctx.channel.method(method)
        _ <- ctx.channel.status.get.assertEquals(Channel.Status.Closed)
        _ <- ctx.rpc.interactions.assert()
      } yield ()
    }
  }

  private val rpcMethods = AllClassesDataGenerator.methods.suchThat {
    case _: (ChannelClass.Close | ChannelClass.CloseOk.type |
          ChannelClass.Flow) =>
      false
    case _ => true
  }

  test("Must handle rpc method responses") {
    forAllF(rpcMethods) { method =>
      for {
        ctx <- LowLevelChannelContext()
        _ <- ctx.channel.method(method)
        _ <- ctx.content.assert(FakeContentChannel.Interaction.Abort)
        _ <- ctx.rpc.interactions.assert(
          FakeRPCChannel.Interaction.Recv(method)
        )
      } yield ()
    }
  }

  import lepus.codecs.ArbitraryDomains.given
  private val channelErrors =
    Gen.resultOf(AMQPError(ReplyCode.NotFound, _, _, _))
  private val generalErrors = Gen.resultOf((s: String) => new Exception(s))

  test("Must close channel on method error : Method") {
    forAllF(rpcMethods, channelErrors) { (method, error) =>
      for {
        ctx <- LowLevelChannelContext()
        _ <- ctx.rpc.error.set(PlannedErrorKind.Times(1, error))
        _ <- ctx.channel.method(method)
        _ <- ctx.channel.status.get.assertEquals(Channel.Status.Closed)
      } yield ()
    }
  }

  test("Must throw underlying errors which are not channel errors : Method") {
    forAllF(rpcMethods, generalErrors) { (method, error) =>
      for {
        ctx <- LowLevelChannelContext()
        _ <- ctx.rpc.error.set(PlannedErrorKind.Times(1, error))
        _ <- ctx.channel.method(method).attempt.assertEquals(Left(error))
        _ <- ctx.channel.status.get.assertEquals(Channel.Status.Closed)
        _ <- ctx.rpc.interactions.assert(
          FakeRPCChannel.Interaction.Recv(method)
        )
      } yield ()
    }
  }

  test("Must throw underlying errors which are not channel errors : Header") {
    forAllF(FrameGenerators.header, channelErrors) { (header, error) =>
      for {
        ctx <- LowLevelChannelContext()
        _ <- ctx.content.error.set(PlannedErrorKind.Times(1, error))
        _ <- ctx.channel.header(header)
        _ <- ctx.channel.status.get.assertEquals(Channel.Status.Closed)
      } yield ()
    }
  }
}

private final case class LowLevelChannelContext(
    channel: LowlevelChannel[IO],
    content: FakeContentChannel,
    rpc: FakeRPCChannel,
    publisher: FakeChannelPublisher,
    dispatcher: FakeMessageDispatcher,
    output: FakeChannelOutput
)
private object LowLevelChannelContext {
  def apply() = for {
    content <- FakeContentChannel()
    rpc <- FakeRPCChannel()
    pub <- FakeChannelPublisher()
    disp <- FakeMessageDispatcher()
    out <- FakeChannelOutput()
    ch <- LowlevelChannel(content, rpc, pub, disp, out)
  } yield new LowLevelChannelContext(ch, content, rpc, pub, disp, out)
}
