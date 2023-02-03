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
import cats.effect.implicits.*
import cats.effect.std.Queue
import cats.effect.std.QueueSource
import cats.implicits.*
import fs2.Pipe
import fs2.Stream
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import lepus.client.apis.*
import lepus.protocol.*
import lepus.protocol.constants.ErrorCode
import lepus.protocol.constants.ErrorType
import lepus.protocol.domains.ChannelNumber

import internal.*
import cats.effect.std.QueueSink
import ConnectionLowLevel.*
import cats.effect.std.Mutex

private[client] trait ConnectionLowLevel[F[_]] {
  def onClosed: F[Unit]
  def onConnected(config: Option[NegotiatedConfig]): F[Unit]

  def onHeader(h: Frame.Header): F[Unit]
  def onBody(b: Frame.Body): F[Unit]
  def onInvoke(m: Frame.Method): F[Unit]

  def addChannel[MC <: MessagingChannel](
      f: ChannelTransmitter[F] => Resource[F, Channel[F, MC]]
  ): Resource[F, Channel[F, MC]]

  def signal: Signal[F, Status]
}

private[client] object ConnectionLowLevel {
  def apply[F[_]: Concurrent](
      send: QueueSink[F, Frame],
      newChannel: (
          ChannelNumber,
          QueueSink[F, Frame]
      ) => Resource[F, LowlevelChannel[F]]
  ): F[ConnectionLowLevel[F]] = for {
    state <- SignallingRef[F].of(Status.Connecting)
    msgDispatch <- MessageDispatcher[F]
  } yield new ConnectionLowLevel[F] {
    def onClosed: F[Unit] = state.set(Status.Closed)
    def onConnected(config: Option[NegotiatedConfig]): F[Unit] = state.update {
      case Status.Connecting =>
        config match {
          case None         => Status.Closed
          case Some(config) => Status.Connected(config)
        }
      case other => other
    }

    private val waitTilEstablished =
      state.discrete
        .flatMap {
          case Status.Connecting   => Stream.empty
          case _: Status.Connected => Stream.unit
          case Status.Closed       => Stream.raiseError(???)
        }
        .head
        .compile
        .resource
        .drain

    private val reserveNextChannelNumber = state
      .modify {
        case s @ Status.Connected(_, _, lastChannel) =>
          val nextChNum = ChannelNumber((lastChannel + 1).toShort)
          (s.copy(lastChannel = nextChNum), nextChNum.some)
        case other => (other, None)
      }
      .flatMap(Concurrent[F].fromOption(_, ???))
      .toResource

    def addChannel[MC <: MessagingChannel](
        f: ChannelTransmitter[F] => Resource[F, Channel[F, MC]]
    ): Resource[F, Channel[F, MC]] = for {
      _ <- waitTilEstablished
      newChNum <- reserveNextChannelNumber
      trm <- LowlevelChannel.from(newChNum, msgDispatch, send).toResource
      ch <- f(trm)
    } yield ch

    def signal: Signal[F, Status] = state

    private def handleError(f: F[Unit | ErrorCode]) = f.flatMap {
      case () => Concurrent[F].unit
      case e: ErrorCode =>
        if e.errorType == ErrorType.Connection then ???
        else ???
    }

    def onHeader(h: Frame.Header): F[Unit] = ???
    def onBody(b: Frame.Body): F[Unit] = ???
    def onInvoke(m: Frame.Method): F[Unit] = ???
  }

  enum Status {
    case Connecting
    case Connected(
        config: NegotiatedConfig,
        channels: Set[ChannelNumber] = Set.empty,
        lastChannel: ChannelNumber = ChannelNumber(0)
    )
    case Closed
  }
}
