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
import lepus.client.internal.FakeConnectionState.Interaction
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

final class FakeConnectionState(
    val interactions: InteractionList[Interaction],
    openedDef: Deferred[IO, Either[Throwable, Unit]],
    val heartbeatError: PlannedError,
    connectedDef: Deferred[IO, NegotiatedConfig]
) extends ConnectionState[IO] {

  override def discrete: Stream[cats.effect.IO, Status] = Stream.empty

  override def awaitOpened: IO[Unit] = openedDef.get.flatMap(IO.fromEither)

  override def onConnected(config: NegotiatedConfig): IO[Unit] =
    connectedDef.complete(config) >> interactions.add(
      Interaction.Connected(config)
    )

  override def onCloseRequest(req: Close): IO[Unit] =
    interactions.add(Interaction.CloseRequest(req))

  override def onCloseRequest: IO[Unit] =
    interactions.add(Interaction.ClientCloseRequest)

  override def get: IO[Status] = ???

  override def onClosed: IO[Unit] = interactions.add(Interaction.Closed)

  override def onOpened: IO[Unit] =
    openedDef.complete(Right(())) >> interactions.add(Interaction.Opened)

  override def config: IO[NegotiatedConfig] = connectedDef.get

  override def continuous: Stream[cats.effect.IO, Status] = Stream.empty

  override def onHeartbeat: IO[Unit] =
    interactions.add(Interaction.Heartbeat) *> heartbeatError.run

  def setAsWontOpen = openedDef.complete(Left(new Exception)).void
}

object FakeConnectionState {
  enum Interaction {
    case Connected(config: NegotiatedConfig)
    case CloseRequest(close: ConnectionClass.Close)
    case ClientCloseRequest, Opened, Closed, Heartbeat
  }

  def apply() = (
    InteractionList[Interaction],
    IO.deferred[Either[Throwable, Unit]],
    PlannedError(),
    IO.deferred[NegotiatedConfig],
  ).mapN(new FakeConnectionState(_, _, _, _))
}
