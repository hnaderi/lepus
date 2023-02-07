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

import cats.effect.*
import cats.effect.implicits.*
import cats.effect.std.Queue
import cats.effect.std.QueueSource
import cats.implicits.*
import fs2.Pipe
import fs2.Stream
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import lepus.client.Connection.Status
import lepus.client.apis.*
import lepus.protocol.*
import lepus.protocol.domains.ChannelNumber
import lepus.protocol.domains.Path

import scala.concurrent.duration.*

import internal.*

trait Connection[F[_]] {
  def channel: Resource[F, Channel[F, NormalMessagingChannel[F]]]
  def reliableChannel
      : Resource[F, Channel[F, ReliablePublishingMessagingChannel[F]]]
  def transactionalChannel
      : Resource[F, Channel[F, TransactionalMessagingChannel[F]]]

  def status: Signal[F, Connection.Status]
  def channels: Signal[F, Set[ChannelNumber]]
}

object Connection {
  def from[F[_]: Temporal](
      transport: Transport[F],
      auth: AuthenticationConfig[F],
      path: Path = Path("/"),
      bufferSize: Int = 100
  ): Resource[F, Connection[F]] = for {
    sendQ <- Resource.eval(Queue.bounded[F, Frame](bufferSize))
    negotiation <- StartupNegotiation(auth, path).toResource
    con <- run2(sendQ, negotiation, transport, path, bufferSize)
  } yield ConnectionImpl[F](con)

  enum Status {
    case Connecting
    case Connected
    case Closed
  }

  private[client] final class ConnectionImpl[F[_]](
      underlying: ConnectionLowLevel[F]
  )(using F: Concurrent[F])
      extends Connection[F] {

    def channel: Resource[F, Channel[F, NormalMessagingChannel[F]]] =
      underlying.newChannel.map(Channel.normal)

    def reliableChannel
        : Resource[F, Channel[F, ReliablePublishingMessagingChannel[F]]] =
      underlying.newChannel.evalMap(Channel.reliable)

    def transactionalChannel
        : Resource[F, Channel[F, TransactionalMessagingChannel[F]]] =
      underlying.newChannel.evalMap(Channel.transactional)

    final def status: Signal[F, Status] = underlying.signal
    final def channels: Signal[F, Set[ChannelNumber]] = underlying.channels
  }

  private[client] def run2[F[_]: Temporal](
      sendQ: Queue[F, Frame],
      negotiation: StartupNegotiation[F],
      transport: Transport[F],
      vhost: Path,
      bufferSize: Int
  ): Resource[F, ConnectionLowLevel[F]] = {
    val frames = Stream
      .fromQueueUnterminated(sendQ, bufferSize)
      .through(transport)
      .through(negotiation.pipe(sendQ.offer))

    val con = negotiation.config.toResource.flatMap {
      case Some(config) =>
        ConnectionLowLevel(
          config,
          vhost,
          sendQ,
          in => LowlevelChannel.from(in.number, in.output)
        )
      case None => ???
    }

    con.flatTap(con => frames.through(con.handler).compile.drain.background)
  }

}
