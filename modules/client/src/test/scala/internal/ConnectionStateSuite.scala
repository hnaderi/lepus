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
import cats.syntax.all.*
import lepus.client.internal.ConnectionState.TerminalState
import lepus.codecs.ConnectionDataGenerator
import lepus.codecs.DomainGenerators
import lepus.protocol.ConnectionClass
import lepus.protocol.Frame
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*
import org.scalacheck.Gen

import java.util.concurrent.TimeoutException
import scala.concurrent.duration.*

import Connection.Status

class ConnectionStateSuite extends InternalTestSuite {
  private val SUT = for {
    out <- OutputWriter[IO, Frame](_ => IO.unit)
    fd <- FrameDispatcher[IO]
    state <- ConnectionState(out, fd)
  } yield state

  private val config = NegotiatedConfig(1, 2, 3)

  test("Initial state is connecting") {
    SUT.flatMap(_.get).assertEquals(Status.Connecting)
  }

  test("Accepts onConnected if is connecting") {
    val configs = Gen.resultOf(NegotiatedConfig(_, _, _))
    forAllF(configs) { config =>
      for {
        s <- SUT
        _ <- s.onConnected(config)
        _ <- s.config.assertEquals(config)
        _ <- s.get.assertEquals(Status.Connected)
      } yield ()
    }
  }

  test("Sends open command onConnected") {
    forAllF(DomainGenerators.path) { vhost =>
      for {
        sent <- FakeFrameOutput()
        fd <- FrameDispatcher[IO]
        s <- ConnectionState(sent, fd, vhost)
        _ <- s.onConnected(config)
        _ <- s.config.assertEquals(config)
        _ <- s.get.assertEquals(Status.Connected)
        _ <- sent.interactions.assert(
          FakeFrameOutput.Interaction.Wrote(
            Frame.Method(ChannelNumber(0), ConnectionClass.Open(vhost))
          )
        )
      } yield ()
    }
  }

  check("config blocks until connected") {
    for {
      s <- SUT
      _ <- s.config.timeout(10.days).intercept[TimeoutException]
      _ <- s.onConnected(config)
      _ <- s.config.assertEquals(config)
    } yield ()
  }

  check("config raises error if closed") {
    for {
      s <- SUT
      _ <- IO.both(
        s.config.intercept[ConnectionState.TerminalState.type],
        s.onClosed.delayBy(10.days)
      )
    } yield ()
  }

  test("Raises error if onConnected is called more than once") {
    for {
      s <- SUT
      _ <- s.onConnected(config)
      _ <- s.onConnected(config).intercept[IllegalStateException]
      _ <- s.get.assertEquals(Status.Connected)
    } yield ()
  }

  check("Accept onOpened if is connected") {
    for {
      s <- SUT
      _ <- s.onConnected(config)
      _ <- s.onOpened
      _ <- s.awaitOpened
      _ <- s.get.assertEquals(Status.Opened)
    } yield ()
  }

  check("awaitOpen waits until state becomes opened") {
    for {
      s <- SUT
      _ <- s.onConnected(config)
      _ <- s.awaitOpened.timeout(10.days).intercept[TimeoutException]
      _ <- s.onOpened
      _ <- s.awaitOpened
      _ <- s.get.assertEquals(Status.Opened)
    } yield ()
  }

  check("awaitOpen raises error if connection get closed") {
    for {
      s <- SUT
      _ <- s.onConnected(config)
      _ <- IO.both(
        s.awaitOpened.intercept[TerminalState.type],
        s.onClosed.delayBy(10.days)
      )
    } yield ()
  }

  test("Raises error if onOpened is called when state is not connected") {
    for {
      s <- SUT
      _ <- s.onOpened.intercept[IllegalStateException]
      _ <- s.get.assertEquals(Status.Connecting)
    } yield ()
  }

  test("Accept onClosed if connecting") {
    for {
      s <- SUT
      _ <- s.onClosed
      _ <- s.get.assertEquals(Status.Closed)
    } yield ()
  }
  test("Accept onClosed if connected") {
    for {
      s <- SUT
      _ <- s.onConnected(config)
      _ <- s.onClosed
      _ <- s.get.assertEquals(Status.Closed)
    } yield ()
  }
  test("Accept onClosed if opened") {
    for {
      s <- SUT
      _ <- s.onConnected(config)
      _ <- s.onOpened
      _ <- s.onClosed
      _ <- s.get.assertEquals(Status.Closed)
    } yield ()
  }

  test("Accepts server close request if is opened") {
    forAllF(ConnectionDataGenerator.closeGen) { close =>
      for {
        sent <- FakeFrameOutput()
        fd <- FrameDispatcher[IO]
        s <- ConnectionState(sent, fd)
        _ <- s.onConnected(config)
        _ <- s.onOpened
        _ <- sent.interactions.reset
        _ <- s.onCloseRequest(close)
        _ <- sent.interactions.assert(
          FakeFrameOutput.Interaction.Wrote(
            Frame.Method(ChannelNumber(0), ConnectionClass.CloseOk)
          )
        )
        _ <- s.get.assertEquals(Status.Opened)
      } yield ()
    }
  }

  test("Accepts client close request if is opened") {
    forAllF(ConnectionDataGenerator.closeGen) { close =>
      for {
        sent <- FakeFrameOutput()
        fd <- FrameDispatcher[IO]
        s <- ConnectionState(sent, fd)
        _ <- s.onConnected(config)
        _ <- s.onOpened
        _ <- sent.interactions.reset
        _ <- s.onCloseRequest
        _ <- sent.interactions.assert(
          FakeFrameOutput.Interaction.Wrote(
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
        )
        _ <- s.get.assertEquals(Status.Opened)
      } yield ()
    }
  }

  test("Responds to heartbeats if is opened") {
    for {
      sent <- FakeFrameOutput()
      fd <- FrameDispatcher[IO]
      s <- ConnectionState(sent, fd)
      _ <- s.onConnected(config)
      _ <- s.onOpened
      _ <- sent.interactions.reset
      _ <- s.onHeartbeat
      _ <- sent.interactions.assert(
        FakeFrameOutput.Interaction.Wrote(Frame.Heartbeat)
      )
    } yield ()
  }

  test("Raises error if onHeartbeat is called and is not opened") {
    for {
      s <- SUT
      _ <- s.onConnected(config)
      _ <- s.onHeartbeat.intercept[IllegalStateException]
    } yield ()
  }

  test("Output terminates after getting closed") {
    for {
      sent <- FakeFrameOutput()
      fd <- FrameDispatcher[IO]
      s <- ConnectionState(sent, fd)
      _ <- s.onClosed
      _ <- sent.interactions.assert(FakeFrameOutput.Interaction.Closed)
    } yield ()
  }

  test("Frame dispatcher terminates after getting closed") {
    for {
      sent <- FakeFrameOutput()
      fd <- FakeFrameDispatcher()
      s <- ConnectionState(sent, fd)
      _ <- fd.assertOpen
      _ <- s.onClosed
      _ <- fd.assertClosed
    } yield ()
  }
}
