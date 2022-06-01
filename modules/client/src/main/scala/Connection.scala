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

import cats.effect.Concurrent
import cats.effect.Resource
import cats.effect.implicits.*
import cats.effect.kernel.Ref
import cats.effect.std.Queue
import cats.implicits.*
import fs2.Pipe
import fs2.Stream
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import lepus.client.Connection.Status
import lepus.client.apis.NormalMessagingChannel
import lepus.client.apis.ReliablePublishingMessagingChannel
import lepus.client.apis.TransactionalMessagingChannel
import lepus.protocol.*
import lepus.protocol.domains.ChannelNumber

import internal.*
import cats.effect.std.QueueSource

trait Connection[F[_]] {
  def channel: Resource[F, Channel[F, NormalMessagingChannel[F]]]
  def reliableChannel
      : Resource[F, Channel[F, ReliablePublishingMessagingChannel[F]]]
  def transactionalChannel
      : Resource[F, Channel[F, TransactionalMessagingChannel[F]]]

  def status: Signal[F, Connection.Status]
  def channels: Signal[F, List[ChannelNumber]]
}

object Connection {
  def from[F[_]: Concurrent](
      transport: Transport[F],
      bufferSize: Int = 100
  ): Resource[F, Connection[F]] = for {
    _status <- Resource.eval(SignallingRef[F].of(Status.New))
    sendQ <- Resource.eval(Queue.bounded[F, Frame](bufferSize))
    con = ConnectionImpl[F](???, ???)
    _ <- run(transport, ???, sendQ, _status, 100).compile.drain.background
  } yield con

  enum Status {
    case New, Connecting, Connected, Closed
  }

  private[client] final class ConnectionImpl[F[_]: Concurrent](
      handler: FrameDispatcher[F],
      mkCh: F[LowlevelChannel[F]]
  ) extends Connection[F] {

    def channel: Resource[F, Channel[F, NormalMessagingChannel[F]]] = for {
      ch <- Resource.eval(mkCh)
      chNr <- handler.add(ch)
      out <- Channel.normal(ch)
    } yield out

    def reliableChannel
        : Resource[F, Channel[F, ReliablePublishingMessagingChannel[F]]] =
      Channel.reliable(???)

    def transactionalChannel
        : Resource[F, Channel[F, TransactionalMessagingChannel[F]]] =
      Channel.transactional(???)

    def status: Signal[F, Connection.Status] = ???
    def channels: Signal[F, List[ChannelNumber]] = ???
  }

  private[client] def run[F[_]: Concurrent](
      transport: Transport[F],
      handler: FrameDispatcher[F],
      sendQ: Queue[F, Frame],
      status: SignallingRef[F, Status],
      bufferSize: Int
  ): Stream[F, Nothing] =
    Stream
      .fromQueueUnterminated(sendQ, bufferSize)
      .through(transport)
      .evalMap {
        case f: Frame.Body   => handler.body(f).void
        case f: Frame.Header => handler.header(f).void
        case m: Frame.Method if m.channel != ChannelNumber(0) =>
          handler.invoke(m).void
        case m: Frame.Method => ???
        case Frame.Heartbeat => sendQ.offer(Frame.Heartbeat)
      }
      .onFinalize(status.set(Status.Closed))
      .drain
}
