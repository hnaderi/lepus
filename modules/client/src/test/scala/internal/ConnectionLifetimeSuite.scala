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
import fs2.Stream.*
import fs2.concurrent.Signal
import lepus.codecs.ConnectionDataGenerator
import lepus.codecs.DomainGenerators
import lepus.codecs.FrameGenerators
import lepus.protocol.BasicClass.Get
import lepus.protocol.BasicClass.Publish
import lepus.protocol.ConnectionClass.Close
import lepus.protocol.*
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*
import org.scalacheck.Gen

import scala.concurrent.duration.*

import Connection.Status

class ConnectionLifetimeSuite extends InternalTestSuite {
  private val config = NegotiatedConfig(1, 2, 3)

  check("sends onConnected when negotation succeeds") {
    for {
      fcon <- FakeConnectionState()
      _ <- fcon.setAsWontOpen
      _ <- Connection.lifetime(IO(config), fcon).compile.drain.attempt
      _ <- fcon.interactions.assertFirst(
        FakeConnectionState.Interaction.Connected(config)
      )
    } yield ()
  }

  check("Must send heartbeats as agreed on negotiation") {
    for {
      fcon <- FakeConnectionState()
      _ <- fcon.onConnected(NegotiatedConfig(1, 1, 60))
      _ <- fcon.onOpened

      _ <- Connection
        .lifetime(IO(config), fcon)
        .compile
        .drain
        .timeout(1.hour + 1.nanosecond)
        .attempt
      _ <- fcon.interactions.assertContainsSlice(
        List.fill(60)(FakeConnectionState.Interaction.Heartbeat): _*
      )
    } yield ()
  }

  // check("waits to become open before") {
  //   for {
  //     fcon <- FakeConnectionState()
  //     _ <- fcon.setAsWontOpen
  //     _ <- Connection.lifetime(IO(config), fcon).compile.drain.attempt
  //     _ <- fcon.interactions.assertFirst(
  //       FakeConnectionState.Interaction.Connected(config)
  //     )
  //   } yield ()
  // }

  // check("Sends Close command on finalization if it is opened") {
  //   for {
  //     fcon <- FakeConnectionState()
  //     _ <- fcon.setAsWontOpen
  //     _ <- Connection.lifetime(IO(config), fcon).compile.drain.attempt
  //     _ <- fcon.interactions.assertFirst(
  //       FakeConnectionState.Interaction.Connected(config)
  //     )
  //   } yield ()
  // }

  //   ConnectionLowLevelContext().use(ctx =>
  //     ctx.con
  //       .handler(ctx.waitWhileOpened)
  //       .compile
  //       .drain >>
  //       ctx.send.data.assertLast(
  //         Frame.Method(
  //           ChannelNumber(0),
  //           ConnectionClass.Close(
  //             ReplyCode.ReplySuccess,
  //             ShortString(""),
  //             ClassId(0),
  //             MethodId(0)
  //           )
  //         )
  //       )
  //   )
  // }

  // test("Closes when server sends connection.close".ignore) {
  //   val closeMethods =
  //     ConnectionDataGenerator.closeGen.map(Frame.Method(ChannelNumber(0), _))

  //   forAllF(closeMethods) { method =>
  //     TestControl.executeEmbed(
  //       ConnectionLowLevelContext().use { ctx =>
  //         ctx.con
  //           .handler(
  //             ctx.waitWhileOpened ++
  //               Stream(method) ++
  //               ctx.waitUntilClosed ++
  //               Stream.exec(
  //                 ctx.con.signal.get.assertEquals(Status.Closed) >>
  //                   ctx.dispatcher.assertNotDispatched >>
  //                   ctx.send.data.assertLast(
  //                     Frame.Method(ChannelNumber(0), ConnectionClass.CloseOk)
  //                   )
  //               )
  //           )
  //           .compile
  //           .drain
  //       }
  //     )
  //   }
  // }

  // test("Closes when server sends connection.close-ok".ignore) {
  //   val method = Frame.Method(ChannelNumber(0), ConnectionClass.CloseOk)

  //   ConnectionLowLevelContext().use(ctx =>
  //     for {
  //       l0 <- CountDownLatch[IO](1)
  //       l1 <- CountDownLatch[IO](1)
  //       handler =
  //         ctx.con
  //           .handler(
  //             ctx.waitWhileOpened ++ Stream(method) ++ Stream.exec(
  //               l0.release >> l1.await
  //             )
  //           )
  //           .compile
  //           .drain
  //       assert = l0.await >>
  //         ctx.con.signal.get.assertEquals(Status.Closed) >>
  //         ctx.dispatcher.assertNotDispatched >>
  //         ctx.send.data.assert(
  //           Frame.Method(ChannelNumber(0), ConnectionClass.Open(Path("/")))
  //         ) >>
  //         l1.release
  //       _ <- IO.both(handler, assert)
  //     } yield ()
  //   )
  // }

  // check("Must send heartbeats as agreed on negotiation".ignore) {
  //   ConnectionLowLevelContext(config = IO(NegotiatedConfig(1, 1, 60))).use(
  //     ctx =>
  //       ctx.con.handler(Stream.sleep_(1.hour + 1.nanosecond)).compile.drain >>
  //         ctx.send.data.assertContainsSlice(List.fill(60)(Frame.Heartbeat): _*)
  //   )
  // }

  // check("Adds channels".ignore) {
  //   val config = IO(NegotiatedConfig(1, 1, 60))

  //   for {
  //     ch <- FakeLowLevelChannel()
  //     builder: ChannelFactory[IO] =
  //       in => LowlevelChannel.from(in.number, in.output).flatMap(ch.setChannel)

  //     server <- ServerScenario.ok
  //     fd <- FrameDispatcher[IO]
  //     con <- ConnectionLowLevel.from(
  //       config,
  //       Path("/"),
  //       fd,
  //       server.messages,
  //       builder
  //     )
  //     handler = con.handler(server.responses).compile.drain
  //     assert = con.newChannel.use(_ =>
  //       ch.interactions.assert(
  //         FakeLowLevelChannel.Interaction.SendWait(ChannelClass.Open)
  //       ) >>
  //         ch.interactions.reset >>
  //         con.channels.get.assertEquals(Set(ChannelNumber(1)))
  //     ) >> ch.interactions.assert(
  //       FakeLowLevelChannel.Interaction.SendWait(
  //         ChannelClass.Close(
  //           ReplyCode.ReplySuccess,
  //           ShortString(""),
  //           ClassId(0),
  //           MethodId(0)
  //         )
  //       )
  //     )

  //     _ <- IO.both(handler, assert)
  //   } yield ()
  // }

  // test("Add channel waits for connection to become opened".ignore) {
  //   for {
  //     built <- IO.ref(false)
  //     builder: ChannelFactory[IO] = _ =>
  //       built.set(true) >> IO.raiseError(new RuntimeException)
  //     _ <- ConnectionLowLevelContext(builder = builder)
  //       .use(ctx =>
  //         IO.both(
  //           ctx.con.newChannel.use_,
  //           built.get.map(!_).assert >>
  //             ctx.con
  //               .handler(
  //                 ctx.waitWhileOpened
  //               )
  //               .compile
  //               .drain >>
  //             built.get.assert
  //         )
  //       )
  //       .intercept[Exception]

  //   } yield ()
  // }

  // check("Add channel fails if connection fails to become opened") {
  //   val builder: ChannelFactory[IO] = _ => IO.raiseError(new RuntimeException)
  //   ConnectionLowLevelContext(builder = builder)
  //     .use(ctx =>
  //       IO.both(
  //         ctx.con.newChannel.use_,
  //         ctx.con.handler(Stream.empty).compile.drain
  //       )
  //     )
  //     .interceptMessage[Exception]("Connection failed")
  // }
}
