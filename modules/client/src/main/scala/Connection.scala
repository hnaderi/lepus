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

  // TODO this prevents unneeded changes for now, but should be removed after replacing types
  private def queueFrom[F[_]: Concurrent](
      f: Frame => F[Unit]
  ): QueueSink[F, Frame] = new {

    override def offer(a: Frame): F[Unit] = f(a)

    override def tryOffer(a: Frame): F[Boolean] = f(a).as(true)

  }

  def from[F[_]: Temporal](
      transport: Transport[F],
      auth: AuthenticationConfig[F],
      path: Path = Path("/"),
      bufferSize: Int = 100
  ): Resource[F, Connection[F]] = for {
    sendQ <- Resource.eval(Queue.bounded[F, Frame](bufferSize))
    negotiation <- StartupNegotiation(auth, path).toResource
    dispatcher <- FrameDispatcher[F].toResource
    state <- ConnectionState(sendQ.offer, path).toResource
    newChannel = ChannelBuilder(
      sendQ.offer,
      state,
      dispatcher,
      in => LowlevelChannel.from(in.number, queueFrom(in.output))
    )

    transfer = Stream
      .fromQueueUnterminated(sendQ, bufferSize)
      .through(transport)
      .through(negotiation.pipe(sendQ.offer))
      .through(receive(state, dispatcher))
    life = lifetime(negotiation.config, state)
    _ <- transfer.merge(life).compile.drain.background
  } yield new {

    override def channels: Signal[F, Set[ChannelNumber]] = dispatcher.channels

    override def status: Signal[F, Status] = state

    override def channel: Resource[F, Channel[F, NormalMessagingChannel[F]]] =
      newChannel.map(Channel.normal)

    override def reliableChannel
        : Resource[F, Channel[F, ReliablePublishingMessagingChannel[F]]] =
      newChannel.evalMap(Channel.reliable)

    override def transactionalChannel
        : Resource[F, Channel[F, TransactionalMessagingChannel[F]]] =
      newChannel.evalMap(Channel.transactional)

  }

  private[client] def receive[F[_]: Concurrent](
      state: ConnectionState[F],
      dispatcher: FrameDispatcher[F]
  ): Pipe[F, Frame, Nothing] = _.foreach {
    case b: Frame.Body   => dispatcher.body(b)
    case h: Frame.Header => dispatcher.header(h)
    case Frame.Method(0, value) =>
      value match {
        case m @ ConnectionClass.OpenOk  => state.onOpened
        case m @ ConnectionClass.CloseOk => state.onClosed
        case m: ConnectionClass.Close    => state.onCloseRequest(m)
        case _                           => ???
      }
    case m: Frame.Method => dispatcher.invoke(m)
    case Frame.Heartbeat => state.onHeartbeat
  }.onFinalize(state.onClosed).interruptWhen(state.whenClosed)

  private[client] def lifetime[F[_]: Temporal](
      config: F[NegotiatedConfig],
      state: ConnectionState[F]
  ): Stream[F, Nothing] = {
    import fs2.Stream.*

    def heartbeats(config: NegotiatedConfig) =
      awakeEvery(config.heartbeat.toInt.seconds)
        .foreach(_ => state.onHeartbeat)

    eval(config)
      .flatMap(config =>
        eval(state.onConnected(config)) >>
          eval(state.awaitOpened) >>
          heartbeats(config)
      )
      .onFinalize(state.onCloseRequest)
      .interruptWhen(state.whenClosed)
  }

  enum Status {
    case Connecting
    case Connected
    case Opened
    case Closed
  }
}
