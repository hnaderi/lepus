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
import lepus.protocol.ChannelClass

class LowLevelChannelSuite extends InternalTestSuite {
  test("Initial status is Active") {
    for {
      ctx <- LowLevelChannelContext()
      _ <- ctx.channel.status.get.assertEquals(Channel.Status.Active)
    } yield ()
  }

  test("Status becomes Closed when .close") {
    for {
      ctx <- LowLevelChannelContext()
      _ <- ctx.channel.close
      _ <- ctx.channel.status.get.assertEquals(Channel.Status.Closed)
    } yield ()
  }

  test("Handles flow control") {
    for {
      ctx <- LowLevelChannelContext()
      _ <- ctx.channel.method(ChannelClass.Flow(false))
      _ <- ctx.channel.status.get.assertEquals(Channel.Status.InActive)
      _ <- ctx.rpc.assert(
        FakeRPCChannel.Interaction.SendNoWait(ChannelClass.FlowOk(false))
      )
      _ <- ctx.rpc.reset
      _ <- ctx.channel.method(ChannelClass.Flow(true))
      _ <- ctx.channel.status.get.assertEquals(Channel.Status.Active)
    } yield ()
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
