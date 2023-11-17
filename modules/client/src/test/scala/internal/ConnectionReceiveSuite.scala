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
import fs2.Stream
import fs2.Stream.*
import lepus.codecs.ConnectionDataGenerator
import lepus.codecs.FrameGenerators
import lepus.protocol.*
import lepus.protocol.domains.*
import org.scalacheck.Gen

import Connection.Status

class ConnectionReceiveSuite extends InternalTestSuite {
  check("Connection become closed when handler terminates") {
    for {
      fd <- FakeFrameDispatcher()
      output <- FakeFrameOutput()
      st <- ConnectionState(output, fd)
      _ <- Stream.empty.through(Connection.receive(st, fd)).compile.drain
      _ <- st.get.assertEquals(Status.Closed)
    } yield ()
  }

  check("Connection become closed when handler terminates with error") {
    for {
      fd <- FakeFrameDispatcher()
      output <- FakeFrameOutput()
      st <- ConnectionState(output, fd)
      _ <- Stream
        .raiseError(new Exception)
        .through(Connection.receive(st, fd))
        .compile
        .drain
        .intercept[Exception]
      _ <- st.get.assertEquals(Status.Closed)
    } yield ()
  }

  private val config = NegotiatedConfig(1, 2, 3)
  check("Responds to pings") {
    for {
      fd <- FakeFrameDispatcher()
      output <- FakeFrameOutput()
      st <- ConnectionState(output, fd)
      _ <- st.onConnected(config)
      _ <- st.onOpened
      _ <- Stream(Frame.Heartbeat)
        .through(Connection.receive(st, fd))
        .compile
        .drain
      _ <- output.interactions.assertContains(
        FakeFrameOutput.Interaction.Wrote(Frame.Heartbeat)
      )
    } yield ()
  }

  test("Dispatches headers and body frames") {
    val frames: Gen[Frame.Body | Frame.Header] =
      Gen.oneOf(FrameGenerators.body, FrameGenerators.header)
    forAllF(frames) { frame =>
      for {
        fd <- FakeFrameDispatcher()
        output <- FakeFrameOutput()
        st <- ConnectionState(output, fd)
        _ <- st.onConnected(config)
        _ <- st.onOpened
        _ <- Stream(frame)
          .through(Connection.receive(st, fd))
          .compile
          .drain
        _ <- fd.dispatched.assert(frame)
      } yield ()
    }
  }

  test("Dispatches all channel level methods") {
    val channelMethods =
      FrameGenerators.method.suchThat(_.channel != ChannelNumber(0))
    forAllF(channelMethods) { method =>
      for {
        fd <- FakeFrameDispatcher()
        output <- FakeFrameOutput()
        st <- ConnectionState(output, fd)
        _ <- st.onConnected(config)
        _ <- st.onOpened
        _ <- Stream(method)
          .through(Connection.receive(st, fd))
          .compile
          .drain
        _ <- fd.dispatched.assert(method)
      } yield ()
    }
  }

  test("Dispatches connection blocked/unblocked notifications") {
    val methods: Gen[ConnectionClass.Blocked | ConnectionClass.Unblocked.type] =
      Gen.oneOf(
        ConnectionDataGenerator.blockedGen,
        ConnectionDataGenerator.unblockedGen
      )

    forAllF(methods) { method =>
      val frame = Frame.Method(ChannelNumber(0), method)
      for {
        fd <- FakeFrameDispatcher()
        output <- FakeFrameOutput()
        st <- FakeConnectionState(Status.Opened())
        _ <- Stream(frame)
          .through(Connection.receive(st, fd))
          .compile
          .drain
        _ <- method match {
          case ConnectionClass.Blocked(msg) =>
            st.interactions.assertFirst(
              FakeConnectionState.Interaction.Blocked(msg)
            )
          case ConnectionClass.Unblocked =>
            st.interactions.assertFirst(
              FakeConnectionState.Interaction.Unblocked
            )
        }
      } yield ()
    }
  }
}
