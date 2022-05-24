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
import cats.implicits.*
import fs2.Pipe
import fs2.Stream
import lepus.protocol.*
import lepus.protocol.classes.*
import lepus.protocol.constants.*
import lepus.protocol.domains.*

sealed trait Messaging[F[_]]

trait Consuming[F[_]] extends Messaging[F] {

  def qos(
      prefetchSize: Int,
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
  ): Stream[F, DeliveredMessage]

  def get(
      queue: QueueName,
      noAck: NoAck
  ): F[Option[SynchronousGet]]

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

trait Publishing[F[_]] extends Messaging[F] {
  def publish(
      exchange: ExchangeName,
      routingKey: ShortString,
      message: Message
  ): F[Unit]

  def publisher: Pipe[F, Envelope, ReturnedMessage]
}

trait ReliablePublishing[F[_]] extends Messaging[F] {
  def publish(env: Envelope): F[ReliableEnvelope[F]]
}

trait DefaultMessaging[F[_]] extends Consuming[F], Publishing[F]
trait ReliableMessaging[F[_]] extends Consuming[F], ReliablePublishing[F]
