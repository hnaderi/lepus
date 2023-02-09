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

  check("sends onConnected when negotation succeeds") {
    val config = NegotiatedConfig(1, 2, 3)
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
    val config = NegotiatedConfig(1, 1, 60)
    for {
      fcon <- FakeConnectionState()

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

  check("waits to become open before sending heartbeats") {
    val config = NegotiatedConfig(1, 1, 60)
    for {
      fcon <- FakeConnectionState()
      _ <- Connection
        .lifetime(IO(config), fcon)
        .compile
        .drain
        .background
        .use(fib =>
          fcon.config >> // wait to become connected
            fcon.interactions
              .assert(FakeConnectionState.Interaction.Connected(config)) >>
            fcon.interactions.reset >>
            fcon.onOpened >> fcon.interactions.reset >> IO.sleep(61.second) >>
            fcon.interactions
              .assert(FakeConnectionState.Interaction.Heartbeat) >>
            fcon.onClosed
        )
    } yield ()
  }
}
