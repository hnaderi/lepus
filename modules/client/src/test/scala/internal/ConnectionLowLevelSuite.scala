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
import lepus.protocol.*

import Connection.Status

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
}
