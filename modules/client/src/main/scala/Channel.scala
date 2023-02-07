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

import cats.MonadError
import cats.effect.Concurrent
import cats.effect.kernel.Resource
import cats.effect.std.Queue
import cats.implicits.*
import fs2.Pipe
import fs2.Stream
import fs2.concurrent.Signal
import lepus.client.Channel.Status
import lepus.client.apis.*
import lepus.client.internal.*
import lepus.protocol.*
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*

trait Channel[F[_], M <: MessagingChannel] {
  def exchange: ExchangeAPI[F]
  def queue: QueueAPI[F]
  def messaging: M

  def status: Signal[F, Status]
}

object Channel {
  enum Status {
    case Active, InActive, Closed
  }
  private final class ChannelImpl[F[_], M <: MessagingChannel](
      transmitter: ChannelTransmitter[F],
      val messaging: M
  )(using MonadError[F, Throwable])
      extends Channel[F, M] {

    override def status: Signal[F, Status] = transmitter.status

    final def exchange: ExchangeAPI[F] = ExchangeAPIImpl(transmitter)
    final def queue: QueueAPI[F] = QueueAPIImpl(transmitter)
  }

  extension [F[_]](rpc: ChannelTransmitter[F]) {
    def call[M <: Method, O](m: M)(using d: RPCCallDef[F, M, O]): F[O] =
      d.call(rpc)(m)
  }

  private abstract class ConsumingImpl[F[_]: Concurrent](
      channel: ChannelTransmitter[F]
  ) extends Consuming[F] {

    def qos(
        prefetchSize: Int,
        prefetchCount: Short,
        global: Boolean
    ): F[BasicClass.QosOk.type] = channel.call(
      BasicClass.Qos(
        prefetchSize = prefetchSize,
        prefetchCount = prefetchCount,
        global = global
      )
    )

    def consume(
        queue: QueueName,
        noLocal: NoLocal = false,
        noAck: NoAck = true,
        exclusive: Boolean = false,
        noWait: NoWait = false,
        arguments: FieldTable = FieldTable.empty
    ): Stream[F, DeliveredMessage] =
      import Stream.*
      val ctag: ConsumerTag = ???
      eval(
        channel.call(
          BasicClass.Consume(
            queue,
            ctag,
            noLocal = noLocal,
            noAck = noAck,
            exclusive = exclusive,
            noWait = noWait,
            arguments
          )
        )
      ) >>
        channel
          .delivered(ctag)
          .onFinalize(
            channel.call(BasicClass.Cancel(ctag, true)).void
          )

    def get(
        queue: QueueName,
        noAck: NoAck
    ): F[Option[SynchronousGet]] =
      channel.get(BasicClass.Get(queue, noAck))

    def ack(deliveryTag: DeliveryTag, multiple: Boolean = false): F[Unit] =
      channel.call(BasicClass.Ack(deliveryTag, multiple))

    def reject(deliveryTag: DeliveryTag, requeue: Boolean = true): F[Unit] =
      channel.call(BasicClass.Reject(deliveryTag, requeue))

    def recoverAsync(requeue: Boolean): F[Unit] =
      channel.call(BasicClass.RecoverAsync(requeue))

    def recover(requeue: Boolean): F[Unit] =
      channel.call(BasicClass.Recover(requeue))

    def nack(
        deliveryTag: DeliveryTag,
        multiple: Boolean = false,
        requeue: Boolean = true
    ): F[Unit] =
      channel.call(
        BasicClass.Nack(deliveryTag, multiple = multiple, requeue = requeue)
      )

  }

  private class NormalPublishingImpl[F[_]: Concurrent](
      channel: ChannelTransmitter[F]
  ) extends ConsumingImpl[F](channel),
        NormalMessagingChannel[F] {
    def publish(
        exchange: ExchangeName,
        routingKey: ShortString,
        message: Message
    ): F[Unit] = channel.publish(
      BasicClass
        .Publish(exchange, routingKey, mandatory = false, immediate = false),
      message
    )

    def publisher: Pipe[F, Envelope, ReturnedMessage] = in =>
      val send = in
        .evalMap(e =>
          channel.publish(
            BasicClass
              .Publish(
                e.exchange,
                e.routingKey,
                mandatory = e.mandatory,
                immediate = false
              ),
            e.message
          )
        )
        .drain

      send.mergeHaltBoth(channel.returned)

  }

  private final class ReliablePublishingImpl[F[_]: Concurrent](
      channel: ChannelTransmitter[F]
  ) extends ConsumingImpl(channel),
        ReliablePublishingMessagingChannel[F] {
    def publish(env: Envelope): F[ReliableEnvelope[F]] = ???
  }

  private final class TransactionalMessagingImpl[F[_]: Concurrent](
      channel: ChannelTransmitter[F]
  ) extends NormalPublishingImpl(channel),
        TransactionalMessagingChannel[F] {
    def transaction: Resource[F, Transaction[F]] = ???
  }

  private[client] def normal[F[_]: Concurrent](
      channel: ChannelTransmitter[F]
  ): Channel[F, NormalMessagingChannel[F]] =
    ChannelImpl(channel, NormalPublishingImpl(channel))

  private[client] def reliable[F[_]: Concurrent](
      channel: ChannelTransmitter[F]
  ): F[Channel[F, ReliablePublishingMessagingChannel[F]]] =
    channel
      .call(ConfirmClass.Select(true))
      .as(ChannelImpl(channel, ReliablePublishingImpl(channel)))

  private[client] def transactional[F[_]: Concurrent](
      channel: ChannelTransmitter[F]
  ): F[Channel[F, TransactionalMessagingChannel[F]]] =
    channel
      .call(TxClass.Select)
      .as(ChannelImpl(channel, TransactionalMessagingImpl(channel)))

}
