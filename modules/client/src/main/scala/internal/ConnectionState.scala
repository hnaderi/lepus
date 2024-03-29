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

import cats.effect.Concurrent
import cats.syntax.all.*
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import lepus.client.Connection.Status
import lepus.protocol.ConnectionClass
import lepus.protocol.ConnectionClass.Close
import lepus.protocol.Frame
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*

import ConnectionState.TerminalState

private[client] trait ConnectionState[F[_]] extends Signal[F, Status] {
  def onConnected(config: NegotiatedConfig): F[Unit]
  def onOpened: F[Unit]
  def onFailed(ex: Throwable): F[Unit]
  def onClosed: F[Unit] = onFailed(TerminalState)
  def onCloseRequest: F[Unit]
  def onCloseRequest(req: ConnectionClass.Close): F[Unit]
  def onHeartbeat: F[Unit]
  def onBlocked(msg: ShortString): F[Unit]
  def onUnblocked: F[Unit]

  def config: F[NegotiatedConfig]
  def awaitOpened: F[Unit]
  final def whenClosed: fs2.Stream[F, Boolean] =
    discrete.map(_ == Status.Closed)
}

private[client] object ConnectionState {
  def apply[F[_]](
      output: OutputWriter[F, Frame],
      dispatcher: FrameDispatcher[F],
      path: Path = Path("/")
  )(using F: Concurrent[F]): F[ConnectionState[F]] = for {
    underlying <- SignallingRef[F, Status](Status.Connecting)
    configDef <- F.deferred[Either[Throwable, NegotiatedConfig]]
    hasOpened <- F.deferred[Either[Throwable, Unit]]
  } yield new {

    override def onConnected(config: NegotiatedConfig): F[Unit] =
      underlying
        .modify {
          case Status.Connecting => (Status.Connected, true)
          case other             => (other, false)
        }
        .ifM(
          configDef.complete(Right(config)) *> output.write(
            Frame.Method(ChannelNumber(0), ConnectionClass.Open(path))
          ),
          F.raiseError(new IllegalStateException)
        )

    override def onCloseRequest(req: Close): F[Unit] =
      output.write(Frame.Method(ChannelNumber(0), ConnectionClass.CloseOk))

    override def onCloseRequest: F[Unit] =
      output.write(
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

    override def onFailed(ex: Throwable): F[Unit] =
      hasOpened.complete(Left(ex)) *>
        configDef.complete(Left(ex)) *>
        output.onClose *>
        dispatcher.onClose *>
        underlying.set(Status.Closed)

    override def onOpened: F[Unit] = hasOpened.complete(Right(())) *> underlying
      .modify {
        case Status.Connected => (Status.Opened(), true)
        case other            => (other, false)
      }
      .ifM(F.unit, F.raiseError(new IllegalStateException))

    override def onHeartbeat: F[Unit] = underlying.get.flatMap {
      case Status.Opened(_) => output.write(Frame.Heartbeat)
      case _                => F.raiseError(new IllegalStateException)
    }

    override def onBlocked(msg: ShortString): F[Unit] = underlying.get.flatMap {
      case Status.Opened(_) => underlying.set(Status.Opened(true))
      case _                => F.raiseError(new IllegalStateException)
    }

    override def onUnblocked: F[Unit] = underlying.get.flatMap {
      case Status.Opened(_) => underlying.set(Status.Opened(false))
      case _                => F.raiseError(new IllegalStateException)
    }

    override def config: F[NegotiatedConfig] =
      configDef.get.flatMap(F.fromEither)

    override def awaitOpened: F[Unit] = hasOpened.get.flatMap(F.fromEither)

    export underlying.{get, discrete, continuous}

  }

  case object TerminalState
      extends Exception(
        "Connection is closed and waiting won't change anything!"
      )
}
