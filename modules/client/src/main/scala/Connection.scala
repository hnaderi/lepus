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
  def apply[F[_]: Concurrent](
      transport: Transport[F],
      bufferSize: Int = 100
  ): Resource[F, Connection[F]] = for {
    _status <- Resource.eval(SignallingRef[F].of(Status.New))
    sendQ <- Resource.eval(Queue.bounded[F, Frame](bufferSize))
    con = ConnectionImpl(sendQ, 100, _status, ???)
    _ <- con.run(transport).compile.drain.background
  } yield con

  enum Status {
    case New, Connecting, Connected, Closed
  }

  private[client] enum State[F[_]] {
    case New()
    case Connected(
        nextChannel: Int,
        channels: Map[ChannelNumber, ChannelReceiver[F]]
    )
  }

  private[client] final class ConnectionImpl[F[_]: Concurrent](
      sendQ: Queue[F, Frame],
      bufferSize: Int,
      _status: SignallingRef[F, Status],
      handler: ConnectionHandler[F]
  ) extends Connection[F] {
    def channel: Resource[F, Channel[F, NormalMessagingChannel[F]]] = ???
    def reliableChannel
        : Resource[F, Channel[F, ReliablePublishingMessagingChannel[F]]] = ???
    def transactionalChannel
        : Resource[F, Channel[F, TransactionalMessagingChannel[F]]] = ???
    def status: Signal[F, Connection.Status] = ???
    def channels: Signal[F, List[ChannelNumber]] = ???

    private[client] def run(transport: Transport[F]): Stream[F, Nothing] =
      Stream
        .fromQueueUnterminated(sendQ, bufferSize)
        .through(transport)
        .evalScan(handler)((h, f) => h.handle(f))
        .onFinalize(_status.set(Status.Closed))
        .drain

  }
}
