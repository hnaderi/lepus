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
import fs2.Stream
import lepus.protocol.*
import lepus.protocol.classes.*
import lepus.protocol.constants.*
import lepus.protocol.domains.*

sealed trait MessagingChannel

trait Consuming[F[_]] {

  def qos(
      prefetchSize: Int = 0,
      prefetchCount: Short,
      global: Boolean
  ): F[BasicClass.QosOk.type]

  def consume(
      queue: QueueName,
      noLocal: NoLocal = false,
      noAck: NoAck = true,
      exclusive: Boolean = false,
      noWait: NoWait = false,
      arguments: FieldTable = FieldTable.empty
  ): Stream[F, DeliveredMessageRaw]

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

  final def publish[T](
      exchange: ExchangeName,
      routingKey: ShortString,
      message: Message[T]
  )(using enc: EnvelopeEncoder[T]): F[Unit] =
    publishRaw(exchange, routingKey, enc.encode(message))

  def publisherRaw: Pipe[F, EnvelopeRaw, ReturnedMessageRaw]

  final def publisher[T](using
      enc: EnvelopeEncoder[T]
  ): Pipe[F, Envelope[T], ReturnedMessageRaw] =
    _.map(enc.encode(_)).through(publisherRaw)
}

trait ReliablePublishing[F[_]] {
  def publishRaw(env: EnvelopeRaw): F[DeliveryTag]
  final def publish[T](env: Envelope[T])(using
      enc: EnvelopeEncoder[T]
  ): F[DeliveryTag] =
    publishRaw(enc.encode(env))
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
