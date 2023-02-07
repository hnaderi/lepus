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

import cats.effect.*
import cats.effect.std.*
import cats.effect.syntax.all.*
import cats.effect.testkit.TestControl
import cats.syntax.all.*
import fs2.Stream
import fs2.concurrent.Signal
import lepus.codecs.ConnectionDataGenerator
import lepus.codecs.DomainGenerators
import lepus.codecs.FrameGenerators
import lepus.protocol.BasicClass.Get
import lepus.protocol.BasicClass.Publish
import lepus.protocol.*
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*
import org.scalacheck.Gen

import scala.concurrent.duration.*

import Connection.Status

class ConnectionLowLevelSuite2 extends InternalTestSuite {
  test("Connection is in connecting state when initiated") {
    ConnectionLowLevelContext().use(
      _.con.signal.get.assertEquals(Status.Connecting)
    )
  }

  test("Connection become closed when handler terminates") {
    ConnectionLowLevelContext().use(ctx =>
      ctx.con.handler(Stream.empty).compile.drain >>
        ctx.con.signal.get.assertEquals(Status.Closed)
    )
  }

  test("Connection become closed when handler terminates with error") {
    ConnectionLowLevelContext().use(ctx =>
      ctx.con
        .handler(Stream.raiseError(new Exception()))
        .compile
        .drain
        .attempt >>
        ctx.con.signal.get.assertEquals(Status.Closed)
    )
  }

  test("Sends Open command on initialization") {
    forAllF(DomainGenerators.path) { vhost =>
      ConnectionLowLevelContext(vhost = vhost).use(ctx =>
        ctx.con.handler(Stream.empty).compile.drain >>
          ctx.send.data.assert(
            Frame.Method(ChannelNumber(0), ConnectionClass.Open(vhost))
          )
      )
    }
  }

  check("Becomes connected when server accepts Open command") {
    ConnectionLowLevelContext().use(ctx =>
      for {
        l0 <- CountDownLatch[IO](1)
        l1 <- CountDownLatch[IO](1)
        handler = ctx.con
          .handler(
            Stream(
              Frame.Method(ChannelNumber(0), ConnectionClass.OpenOk)
            ) ++ Stream.exec(l0.release >> l1.await)
          )
          .compile
          .drain
        assert = l0.await >> ctx.con.signal.get
          .assertEquals(Status.Connected2) >> l1.release
        _ <- IO.both(handler, assert)
      } yield ()
    )
  }

  test("Sends Close command on finalization") {
    val config = NegotiatedConfig(1, 1, 60)
    val builderStub: ChannelFactory[IO] = _ => ???
    for {
      q <- FakeFrameOutput()
      fd <- FakeFrameDispatcher()
      _ <- ConnectionLowLevel2
        .from(config, Path("/"), fd, q, builderStub)
        .use(_ => q.data.reset)
      _ <- q.data.assert(
        Frame.Method(
          ChannelNumber(0),
          ConnectionClass.Close(
            ReplyCode.ReplySuccess,
            ShortString(""),
            ClassId(0),
            MethodId(0)
          )
        )
      )
    } yield ()
  }

  test("Responds to pings") {
    ConnectionLowLevelContext().use(ctx =>
      ctx.send.data.reset >>
        ctx.con.handler(Stream(Frame.Heartbeat)).compile.drain >>
        ctx.send.data.assert(Frame.Heartbeat)
    )
  }

  test("Dispatches headers and body frames") {
    val frames: Gen[Frame.Body | Frame.Header] =
      Gen.oneOf(FrameGenerators.body, FrameGenerators.header)
    forAllF(frames) { frame =>
      ConnectionLowLevelContext().use(ctx =>
        ctx.con.handler(Stream(frame)).compile.drain >>
          ctx.dispatcher.assertDispatched(frame)
      )
    }
  }

  test("Dispatches all channel level methods") {
    val channelMethods =
      FrameGenerators.method.suchThat(_.channel != ChannelNumber(0))
    forAllF(channelMethods) { method =>
      ConnectionLowLevelContext().use(ctx =>
        ctx.con.handler(Stream(method)).compile.drain >>
          ctx.dispatcher.assertDispatched(method)
      )
    }
  }

  test("Closes when server sends connection.close") {
    val closeMethods =
      ConnectionDataGenerator.closeGen.map(Frame.Method(ChannelNumber(0), _))

    forAllF(closeMethods) { method =>
      ConnectionLowLevelContext().use(ctx =>
        for {
          l0 <- CountDownLatch[IO](1)
          l1 <- CountDownLatch[IO](1)
          handler = ctx.send.data.reset >>
            ctx.con
              .handler(Stream(method) ++ Stream.exec(l0.release >> l1.await))
              .compile
              .drain
          assert = l0.await >>
            ctx.con.signal.get.assertEquals(Status.Closed) >>
            ctx.dispatcher.assertNotDispatched >>
            ctx.send.data.assert(
              Frame.Method(ChannelNumber(0), ConnectionClass.CloseOk)
            ) >>
            l1.release
          _ <- IO.both(handler, assert)
        } yield ()
      )
    }
  }

  test("Closes when server sends connection.close-ok") {
    val method = Frame.Method(ChannelNumber(0), ConnectionClass.CloseOk)

    ConnectionLowLevelContext().use(ctx =>
      for {
        l0 <- CountDownLatch[IO](1)
        l1 <- CountDownLatch[IO](1)
        handler = ctx.send.data.reset >>
          ctx.con
            .handler(Stream(method) ++ Stream.exec(l0.release >> l1.await))
            .compile
            .drain
        assert = l0.await >>
          ctx.con.signal.get.assertEquals(Status.Closed) >>
          ctx.dispatcher.assertNotDispatched >>
          ctx.send.data.assert() >>
          l1.release
        _ <- IO.both(handler, assert)
      } yield ()
    )
  }

  check("Must send heartbeats as agreed on negotiation") {
    ConnectionLowLevelContext(config = NegotiatedConfig(1, 1, 60)).use(ctx =>
      ctx.send.data.reset >>
        ctx.con.handler(Stream.sleep_(1.hour + 1.nanosecond)).compile.drain >>
        ctx.send.data.all.assertEquals(List.fill(60)(Frame.Heartbeat))
    )
  }

  check("Adds channels") {
    val scenario = for {
      openResp <- IO.deferred[Frame]
      closeResp <- IO.deferred[Frame]

      serverResponses =
        Stream(Frame.Method(ChannelNumber(0), ConnectionClass.OpenOk)) ++
          Stream.eval(openResp.get) ++ Stream.eval(closeResp.get)

      messages = new QueueSink[IO, Frame] {

        override def offer(a: Frame): IO[Unit] = a match {
          case Frame.Method(channel, ChannelClass.Open) =>
            openResp.complete(Frame.Method(channel, ChannelClass.OpenOk)).void
          case Frame.Method(channel, _: ChannelClass.Close) =>
            closeResp.complete(Frame.Method(channel, ChannelClass.CloseOk)).void
          case _ => IO.unit
        }

        override def tryOffer(a: Frame): IO[Boolean] = offer(a).as(true)

      }

    } yield ServerScenario(serverResponses, messages)

    val config = NegotiatedConfig(1, 1, 60)

    for {
      ch <- FakeLowLevelChannel()
      builder: ChannelFactory[IO] =
        in => LowlevelChannel.from(in.number, in.output).flatMap(ch.setChannel)

      server <- scenario
      fd <- FrameDispatcher[IO]
      _ <- ConnectionLowLevel2
        .from(config, Path("/"), fd, server.messages, builder)
        .use { con =>
          val handler = con.handler(server.responses).compile.drain
          val assert = con.newChannel.use(_ =>
            ch.interactions.assert(
              FakeLowLevelChannel.Interaction.SendWait(ChannelClass.Open)
            ) >>
              ch.interactions.reset >>
              con.channels.get.assertEquals(Set(ChannelNumber(1)))
          ) >> ch.interactions.assert(
            FakeLowLevelChannel.Interaction.SendWait(
              ChannelClass.Close(
                ReplyCode.ReplySuccess,
                ShortString(""),
                ClassId(0),
                MethodId(0)
              )
            )
          )

          IO.both(handler, assert)
        }
    } yield ()
  }

  test("Add channel waits for connection to become connected") {
    val error = new RuntimeException
    for {
      built <- IO.ref(false)
      builder: ChannelFactory[IO] = _ => built.set(true) >> IO.raiseError(error)
      _ <- ConnectionLowLevelContext(builder = builder)
        .use(ctx =>
          IO.both(
            ctx.con.newChannel.use_,
            built.get.map(!_).assert >>
              ctx.con
                .handler(
                  Stream(Frame.Method(ChannelNumber(0), ConnectionClass.OpenOk))
                )
                .compile
                .drain >>
              built.get.assert
          )
        )
        .attempt
        .assertEquals(Left(error))
    } yield ()
  }

  check("Add channel fails if connection fails to become connected") {
    val builder: ChannelFactory[IO] = _ => IO.raiseError(new RuntimeException)
    ConnectionLowLevelContext(builder = builder)
      .use(ctx =>
        IO.both(
          ctx.con.newChannel.use_,
          ctx.con.handler(Stream.empty).compile.drain
        )
      )
      .interceptMessage[Exception]("Connection failed")
  }
}

private final case class ConnectionLowLevelContext(
    send: FakeFrameOutput,
    dispatcher: FakeFrameDispatcher,
    con: ConnectionLowLevel2[IO]
)

private object ConnectionLowLevelContext {
  private val defaultConfig = NegotiatedConfig(1, 1, 60)
  private val builderStub: ChannelFactory[IO] = _ => ???

  def apply(
      config: NegotiatedConfig = defaultConfig,
      vhost: Path = Path("/"),
      builder: ChannelFactory[IO] = builderStub
  ) = for {
    q <- FakeFrameOutput().toResource
    fd <- FakeFrameDispatcher().toResource
    con <- ConnectionLowLevel2.from(config, vhost, fd, q, builder)
  } yield new ConnectionLowLevelContext(q, fd, con)
}

final case class ServerScenario(
    responses: Stream[IO, Frame],
    messages: QueueSink[IO, Frame]
)
