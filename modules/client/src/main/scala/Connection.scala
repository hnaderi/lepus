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
import internal.ConnectionLowLevel

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
    state <- Resource.eval(
      ConnectionLowLevel(sendQ, LowlevelChannel.from[F](_, _).toResource)
    )
    negotiation <- StartupNegotiation(auth, path).toResource
    con = ConnectionImpl[F](state)
    _ <- run(
      sendQ,
      negotiation,
      transport,
      state,
      bufferSize
    ).compile.drain.background
  } yield con

  enum Status {
    case Connecting
    case Connected(config: NegotiatedConfig)
    case Closed
  }

  private[client] final class ConnectionImpl[F[_]](
      underlying: ConnectionLowLevel[F]
  )(using F: Concurrent[F])
      extends Connection[F] {

    def channel: Resource[F, Channel[F, NormalMessagingChannel[F]]] =
      underlying.addChannel(Channel.normal)

    def reliableChannel
        : Resource[F, Channel[F, ReliablePublishingMessagingChannel[F]]] =
      underlying.addChannel(Channel.reliable)

    def transactionalChannel
        : Resource[F, Channel[F, TransactionalMessagingChannel[F]]] =
      underlying.addChannel(Channel.transactional)

    final def status: Signal[F, Status] = underlying.signal
    final def channels: Signal[F, Set[ChannelNumber]] = underlying.channels
  }

  private[client] def run[F[_]: Temporal](
      sendQ: Queue[F, Frame],
      negotiation: StartupNegotiation[F],
      transport: Transport[F],
      state: ConnectionLowLevel[F],
      bufferSize: Int
  ): Stream[F, Nothing] = {
    val frames = Stream
      .fromQueueUnterminated(sendQ, bufferSize)
      .through(transport)
      .through(negotiation.pipe(sendQ.offer))

    val statusHandler =
      Stream.eval(negotiation.config).evalMap(state.onConnected)

    val heartbeats = state.signal.discrete.flatMap {
      case Status.Connected(config) =>
        Stream
          .awakeEvery[F](config.heartbeat.toInt.seconds)
          .foreach(_ => sendQ.offer(Frame.Heartbeat))
      case _ => Stream.empty
    }

    val frameHandler = frames.foreach {
      case f: Frame.Body   => state.onBody(f)
      case f: Frame.Header => state.onHeader(f)
      case m: Frame.Method if m.channel != ChannelNumber(0) =>
        state.onInvoke(m)
      case m: Frame.Method => // Global methods like connection class
        import lepus.protocol.ConnectionClass.*
        m.value match {
          case Close(replyCode, replyText, classId, methodId) =>
            state.onClosed // TODO send close ok
          case CloseOk                         => state.onClosed
          case Blocked(reason)                 => ???
          case Unblocked                       => ???
          case UpdateSecret(newSecret, reason) => ???
          case UpdateSecretOk                  => ???
          case _                               => ??? // TODO unexpected frame
        }
      case Frame.Heartbeat => sendQ.offer(Frame.Heartbeat)
    }

    frameHandler
      .concurrently(statusHandler)
      .concurrently(heartbeats)
      .onFinalize(state.onClosed) // TODO closing might need extra work
  }

}
