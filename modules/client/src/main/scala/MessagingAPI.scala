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
package apis

import cats.MonadError
import cats.effect.kernel.Resource
import cats.implicits.*
import fs2.Pipe
import fs2.RaiseThrowable
import fs2.Stream
import fs2.compat.NotGiven
import lepus.protocol.*
import lepus.protocol.classes.*
import lepus.protocol.constants.*
import lepus.protocol.domains.*

sealed trait MessagingChannel

trait Consuming[F[_]] {

  def qos(
      prefetchSize: Int = 0,
      prefetchCount: Short,
      global: Boolean = false
  ): F[BasicClass.QosOk.type]

  def consumeRaw(
      queue: QueueName,
      noLocal: NoLocal = false,
      noAck: NoAck = true,
      exclusive: Boolean = false,
      arguments: FieldTable = FieldTable.empty
  ): Stream[F, DeliveredMessageRaw]

  final def consume[T](
      queue: QueueName,
      mode: ConsumeMode = ConsumeMode.RaiseOnError(false),
      noLocal: NoLocal = false,
      exclusive: Boolean = false,
      arguments: FieldTable = FieldTable.empty
  )(using
      dec: EnvelopeDecoder[T],
      F: RaiseThrowable[F]
  ): Stream[F, DeliveredMessage[T]] = {
    val noAck = mode == ConsumeMode.RaiseOnError(false)
    val run: DeliveredMessageRaw => Stream[F, DeliveredMessage[T]] =
      mode match {
        case ConsumeMode.RaiseOnError(_) =>
          msg =>
            Stream.fromEither(
              dec.decode(msg.message).map(n => msg.copy(message = n))
            )
        case ConsumeMode.NackOnError =>
          msg =>
            dec
              .decode(msg.message)
              .map(n => msg.copy(message = n))
              .fold(
                _ => Stream.exec(nack(msg.deliveryTag, false, false)),
                Stream.emit(_)
              )
      }

    consumeRaw(queue, noLocal, noAck, exclusive, arguments).flatMap(run)
  }

  def get(
      queue: QueueName,
      noAck: NoAck
  ): F[Option[SynchronousGetRaw]]

  def ack(deliveryTag: DeliveryTag, multiple: Boolean = false): F[Unit]

  def reject(deliveryTag: DeliveryTag, requeue: Boolean = true): F[Unit]

  def recoverAsync(requeue: Boolean): F[Unit]

  def recover(requeue: Boolean): F[Unit]

  def nack(
      deliveryTag: DeliveryTag,
      multiple: Boolean = false,
      requeue: Boolean = true
  ): F[Unit]

}

trait Publishing[F[_]] {
  def publishRaw(
      exchange: ExchangeName,
      routingKey: ShortString,
      message: MessageRaw
  ): F[Unit]

  final inline def publish[T](
      exchange: ExchangeName,
      routingKey: ShortString,
      message: Message[T]
  )(using enc: EnvelopeEncoder[T]): F[Unit] =
    publishRaw(exchange, routingKey, enc.encode(message))

  final inline def publish[T](
      exchange: ExchangeName,
      routingKey: ShortString,
      payload: T
  )(using enc: EnvelopeEncoder[T])(using NotGiven[T <:< Message[?]]): F[Unit] =
    publish(exchange, routingKey, Message(payload))

  def publisherRaw: Pipe[F, EnvelopeRaw, ReturnedMessageRaw]

  final inline def publisher[T: EnvelopeEncoder]
      : Pipe[F, Envelope[T], ReturnedMessageRaw] =
    _.map(_.toRaw).through(publisherRaw)
}

trait ReliablePublishing[F[_]] {
  protected def publishRaw(env: EnvelopeRaw): F[DeliveryTag]

  final inline def publishRaw(
      exchange: ExchangeName,
      routingKey: ShortString,
      message: MessageRaw
  ): F[DeliveryTag] = publishRaw(
    EnvelopeRaw(exchange, routingKey, mandatory = false, message)
  )

  final inline def publish[T: EnvelopeEncoder](
      exchange: ExchangeName,
      routingKey: ShortString,
      message: Message[T]
  ): F[DeliveryTag] =
    publishRaw(exchange, routingKey, message.toRaw)

  final inline def publish[T: EnvelopeEncoder](
      env: Envelope[T]
  ): F[DeliveryTag] =
    publishRaw(env.toRaw)

  def publisherRaw: Pipe[F, EnvelopeRaw, DeliveryTag | ReturnedMessageRaw]

  final inline def publisher[T: EnvelopeEncoder]
      : Pipe[F, Envelope[T], DeliveryTag | ReturnedMessageRaw] =
    _.map(_.toRaw).through(publisherRaw)

  def confirmations: Stream[F, Confirmation]
}

trait Transaction[F[_]] {
  def commit: F[Unit]
  def rollback: F[Unit]
}

trait TransactionalMessaging[F[_]] {
  def transaction: Resource[F, Transaction[F]]
}

trait NormalMessagingChannel[F[_]]
    extends MessagingChannel,
      Consuming[F],
      Publishing[F]
trait ReliablePublishingMessagingChannel[F[_]]
    extends MessagingChannel,
      Consuming[F],
      ReliablePublishing[F]
trait TransactionalMessagingChannel[F[_]]
    extends MessagingChannel,
      NormalMessagingChannel[F],
      TransactionalMessaging[F]
