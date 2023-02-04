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
import cats.effect.std.Queue
import lepus.client.internal.ConnectionLowLevel
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*
import lepus.codecs.FrameGenerators
import lepus.protocol.*
import org.scalacheck.Gen

import Connection.Status
import cats.effect.testkit.TestControl
import lepus.protocol.constants.ReplyCategory

class ConnectionLowLevelSuite extends InternalTestSuite {
  test("Connection is in connecting state when initiated") {
    for {
      q <- Queue.synchronous[IO, Frame]
      fd <- FrameDispatcher[IO]
      con <- ConnectionLowLevel(q, (_, _) => ???)
      _ <- con.signal.get.assertEquals(Status.Connecting)
    } yield ()
  }

  test("Connection become closed when onClosed is called") {
    for {
      q <- Queue.synchronous[IO, Frame]
      fd <- FrameDispatcher[IO]
      con <- ConnectionLowLevel(q, (_, _) => ???)
      _ <- con.onClosed
      _ <- con.signal.get.assertEquals(Status.Closed)
    } yield ()
  }

  test("Connection become closed when negotiation fails") {
    for {
      q <- Queue.synchronous[IO, Frame]
      fd <- FrameDispatcher[IO]
      con <- ConnectionLowLevel(q, (_, _) => ???)
      _ <- con.onConnected(None)
      _ <- con.signal.get.assertEquals(Status.Closed)
    } yield ()
  }

  test("Connection become connected when negotiation succeeds") {
    for {
      q <- Queue.synchronous[IO, Frame]
      fd <- FrameDispatcher[IO]
      con <- ConnectionLowLevel(q, (_, _) => ???)
      _ <- con.onConnected(Some(NegotiatedConfig(1, 2, 3)))
      _ <- con.signal.get.assertEquals(
        Status.Connected(NegotiatedConfig(1, 2, 3))
      )
    } yield ()
  }

  private val frames = Gen.oneOf(
    FrameGenerators.header,
    FrameGenerators.body,
    FrameGenerators.method
  )

  test("Dispatches frames") {
    forAllF(frames) { frame =>
      for {
        q <- Queue.synchronous[IO, Frame]
        fd <- FakeFrameDispatcher()
        con <- ConnectionLowLevel.from(q, fd, (_, _) => ???)
        _ <- frame match {
          case m: Frame.Method => con.onInvoke(m)
          case h: Frame.Header => con.onHeader(h)
          case b: Frame.Body   => con.onBody(b)
          case Frame.Heartbeat => ??? // does not happen
        }

        _ <- fd.assertDispatched(frame)
      } yield ()
    }
  }

  test("Sends channel/connection close on failures") {
    import lepus.codecs.ArbitraryDomains.given
    val protocolErrors = Gen.resultOf(AMQPError(_, _, _, _))
    val exceptions = Gen.resultOf((s: String) => new Exception(s))
    val errors = Gen.oneOf(protocolErrors, exceptions)

    forAllF(frames, errors) { (frame, error) =>
      TestControl.executeEmbed(
        for {
          q <- Queue.bounded[IO, Frame](1)
          fd <- FakeFrameDispatcher(Some(error))
          con <- ConnectionLowLevel.from(q, fd, (_, _) => ???)
          (ch, send) = frame match {
            case m: Frame.Method => (m.channel, con.onInvoke(m))
            case h: Frame.Header => (h.channel, con.onHeader(h))
            case b: Frame.Body   => (b.channel, con.onBody(b))
            case Frame.Heartbeat => ??? // does not happen
          }
          _ <- send

          expected = error match {
            case AMQPError(replyCode, replyText, classId, methodId) =>
              if replyCode.category == ReplyCategory.ChannelError
              then
                Frame.Method(
                  ch,
                  ChannelClass.Close(replyCode, replyText, classId, methodId)
                )
              else
                Frame.Method(
                  ch,
                  ConnectionClass.Close(replyCode, replyText, classId, methodId)
                )
            case _ =>
              Frame.Method(
                ch,
                ConnectionClass.Close(
                  ReplyCode.InternalError,
                  ShortString(""),
                  ClassId(0),
                  MethodId(0)
                )
              )
          }

          _ <- q.take.assertEquals(expected)

        } yield ()
      )
    }
  }
}
