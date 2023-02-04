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
import cats.effect.std.Mutex
import cats.effect.std.Queue
import cats.effect.std.QueueSink
import cats.effect.std.QueueSource
import cats.implicits.*
import fs2.Pipe
import fs2.Stream
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import lepus.client.Connection.Status
import lepus.client.apis.*
import lepus.protocol.*
import lepus.protocol.constants.ReplyCategory
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*

import internal.*
import ConnectionLowLevel.*

private[client] trait ConnectionLowLevel[F[_]] {
  def onClosed: F[Unit]
  def onConnected(config: Option[NegotiatedConfig]): F[Unit]

  def onHeader(h: Frame.Header): F[Unit]
  def onBody(b: Frame.Body): F[Unit]
  def onInvoke(m: Frame.Method): F[Unit]

  def addChannel[MC <: MessagingChannel](
      f: ChannelTransmitter[F] => Resource[F, Channel[F, MC]]
  ): Resource[F, Channel[F, MC]]

  def signal: Signal[F, Connection.Status]
  def channels: Signal[F, Set[ChannelNumber]]
}

private[client] object ConnectionLowLevel {
  def apply[F[_]: Concurrent](
      send: QueueSink[F, Frame],
      newChannel: (
          ChannelNumber,
          QueueSink[F, Frame]
      ) => Resource[F, LowlevelChannel[F]]
  ): F[ConnectionLowLevel[F]] =
    FrameDispatcher[F].flatMap(from(send, _, newChannel))

  def from[F[_]: Concurrent](
      send: QueueSink[F, Frame],
      frameDispatcher: FrameDispatcher[F],
      newChannel: (
          ChannelNumber,
          QueueSink[F, Frame]
      ) => Resource[F, LowlevelChannel[F]]
  ): F[ConnectionLowLevel[F]] = for {
    state <- SignallingRef[F].of(Status.Connecting)
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
          case Status.Closed       => Stream.raiseError(new Exception())
        }
        .head
        .compile
        .resource
        .drain

    def addChannel[MC <: MessagingChannel](
        f: ChannelTransmitter[F] => Resource[F, Channel[F, MC]]
    ): Resource[F, Channel[F, MC]] = for {
      _ <- waitTilEstablished
      // TODO build using negotiated config
      trm <- frameDispatcher.add(newChannel(_, send))
      ch <- f(trm)
    } yield ch

    def signal: Signal[F, Status] = state
    def channels: Signal[F, Set[ChannelNumber]] = frameDispatcher.channels

    private def handleError(chNum: ChannelNumber, f: F[Unit]) =
      f.handleErrorWith {
        case AMQPError(replyCode, replyText, classId, methodId) =>
          replyCode.category match {
            case ReplyCategory.ChannelError =>
              send.offer(
                Frame.Method(
                  chNum,
                  ChannelClass.Close(replyCode, replyText, classId, methodId)
                )
              )
            case _ =>
              send.offer(
                Frame.Method(
                  chNum,
                  ConnectionClass.Close(replyCode, replyText, classId, methodId)
                )
              )
          }
        case other =>
          send.offer(
            Frame.Method(
              chNum,
              ConnectionClass.Close(
                ReplyCode.InternalError,
                ShortString(""),
                ClassId(0),
                MethodId(0)
              )
            )
          )
      }

    def onHeader(h: Frame.Header): F[Unit] =
      handleError(h.channel, frameDispatcher.header(h))
    def onBody(b: Frame.Body): F[Unit] =
      handleError(b.channel, frameDispatcher.body(b))
    def onInvoke(m: Frame.Method): F[Unit] =
      handleError(m.channel, frameDispatcher.invoke(m))
  }
}
