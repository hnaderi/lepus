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

import cats.effect.Concurrent
import cats.effect.kernel.Resource
import fs2.Pipe
import fs2.RaiseThrowable
import fs2.Stream
import fs2.compat.NotGiven
import lepus.protocol.*
import lepus.protocol.domains.*

sealed trait MessagingChannel

trait Consuming[F[_]] {

  def qos(
      prefetchSize: Int = 0,
      prefetchCount: Short,
      global: Boolean = false
  ): F[BasicClass.QosOk.type]

  /** Consumes raw messages */
  def consumeRaw(
      queue: QueueName,
      noLocal: NoLocal = false,
      noAck: NoAck = true,
      exclusive: Boolean = false,
      arguments: FieldTable = FieldTable.empty,
      consumerTag: Option[ConsumerTag] = None
  ): Stream[F, DeliveredMessageRaw]

  /** Consumes and decodes messages
    *
    * Note that you MUST acknowledge (ack, reject, nack) messages if you select
    * any [[ConsumeMode]] but ConsumeMode.RaiseOnError(false)
    *
    * @param queue
    *   QueueName to consume from
    * @param mode
    *   what to do when a message cannot be decoded
    * @param noLocal
    *   don't consume messages published on this connection
    * @param exclusive
    *   request exclusive consumer right
    * @param arguments
    *   extra params
    * @param consumerTag
    *   add consumer tag, default tag is UUID
    *
    * @returns
    *   successfully decoded messages
    */
  final def consume[T](
      queue: QueueName,
      mode: ConsumeMode = ConsumeMode.RaiseOnError(false),
      noLocal: NoLocal = false,
      exclusive: Boolean = false,
      arguments: FieldTable = FieldTable.empty,
      consumerTag: Option[ConsumerTag] = None
  )(using
      dec: MessageDecoder[T],
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

    consumeRaw(queue, noLocal, noAck, exclusive, arguments, consumerTag)
      .flatMap(run)
  }

  def get(
      queue: QueueName,
      noAck: NoAck = true
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

  /** Publishes raw envelope, this is a low level operation and is exposed only
    * for special circumstances, always prefer to use other higher level publish
    * methods
    *
    * Note that if a mandatory message fails in routing, it is returned to the
    * client and you MUST also consume [[returned]] if you publish mandatory
    * messages
    */
  def publishRaw(env: EnvelopeRaw): F[Unit]

  /** Encodes and publishes an envelope, this is a low level operation and is
    * exposed only for special circumstances, always prefer to use other higher
    * level publish methods
    *
    * Note that if a mandatory message fails in routing, it is returned to the
    * client and you MUST also consume [[returned]] if you publish mandatory
    * messages
    */
  final inline def publish[T: MessageEncoder](env: Envelope[T]): F[Unit] =
    publishRaw(env.toRaw)

  /** Publishes raw message that is not mandatory This is useful if you have
    * handled encoding and want to publish a raw message directly
    */
  final inline def publishRaw(
      exchange: ExchangeName,
      routingKey: ShortString,
      message: MessageRaw
  ): F[Unit] = publishRaw(
    EnvelopeRaw(exchange, routingKey, mandatory = false, message)
  )

  /** Encodes and publishes a message that is not mandatory */
  final inline def publish[T: MessageEncoder](
      exchange: ExchangeName,
      routingKey: ShortString,
      message: Message[T]
  ): F[Unit] =
    publishRaw(exchange, routingKey, message.toRaw)

  /** Creates a message with given payload, encodes it and then publishes it as
    * not mandatory
    */
  final inline def publish[T](
      exchange: ExchangeName,
      routingKey: ShortString,
      payload: T
  )(using enc: MessageEncoder[T])(using NotGiven[T <:< Message[?]]): F[Unit] =
    publish(exchange, routingKey, Message(payload))

  /** A pipe that publishes [[Envelope]]s that may or may not be mandatory, And
    * [[ReturnedMessageRaw]] for returned messages.
    *
    * Note that this pipe SHOULD be used exclusively, as it is draining from the
    * returned messages that is backed by a queue.
    */
  final def publisherRaw(using
      Concurrent[F]
  ): Pipe[F, EnvelopeRaw, ReturnedMessageRaw] =
    _.foreach(publishRaw(_)).mergeHaltBoth(returned)

  /** A pipe that encodes and publishes [[Envelope]]s that may or may not be
    * mandatory, And [[ReturnedMessageRaw]] for returned messages.
    *
    * Note that this pipe SHOULD be used exclusively, as it is draining from the
    * returned messages that is backed by a queue.
    */
  final def publisher[T: MessageEncoder](using
      F: Concurrent[F]
  ): Pipe[F, Envelope[T], ReturnedMessageRaw] =
    _.map(_.toRaw).through(publisherRaw)

  /** consumes returned messages from the server, this should be used
    * exclusively, or otherwise different instances compete over received
    * values, and each get different set of values.
    *
    * Also note that this is a low level operation that is exposed for special
    * circumstances, always prefer to use publisher pipe instead, unless
    * necessary
    */
  def returned: Stream[F, ReturnedMessageRaw]
}

trait ReliablePublishing[F[_]] {

  /** Publishes raw envelope, this is a low level operation and is exposed only
    * for special circumstances, always prefer to use other higher level publish
    * methods
    *
    * Note that if a mandatory message fails in routing, it is returned to the
    * client and you MUST also consume [[returned]] if you publish mandatory
    * messages
    *
    * @returns
    *   DeliveryTag for this message, which you should keep until acked or
    *   nacked from the server
    */
  def publishRaw(env: EnvelopeRaw): F[DeliveryTag]

  /** Encodes and publishes an envelope, this is a low level operation and is
    * exposed only for special circumstances, always prefer to use other higher
    * level publish methods
    *
    * Note that if a mandatory message fails in routing, it is returned to the
    * client and you MUST also consume [[returned]] if you publish mandatory
    * messages
    *
    * @returns
    *   DeliveryTag for this message, which you should keep until acked or
    *   nacked from the server
    */
  final inline def publish[T: MessageEncoder](
      env: Envelope[T]
  ): F[DeliveryTag] =
    publishRaw(env.toRaw)

  /** Publishes raw message that is not mandatory This is useful if you have
    * handled encoding and want to publish a raw message directly
    *
    * @returns
    *   DeliveryTag for this message, which you should keep until acked or
    *   nacked from the server
    */
  final inline def publishRaw(
      exchange: ExchangeName,
      routingKey: ShortString,
      message: MessageRaw
  ): F[DeliveryTag] = publishRaw(
    EnvelopeRaw(exchange, routingKey, mandatory = false, message)
  )

  /** Encodes and publishes a message that is not mandatory
    *
    * @returns
    *   DeliveryTag for this message, which you should keep until acked or
    *   nacked from the server
    */
  final inline def publish[T: MessageEncoder](
      exchange: ExchangeName,
      routingKey: ShortString,
      message: Message[T]
  ): F[DeliveryTag] =
    publishRaw(exchange, routingKey, message.toRaw)

  /** Creates a message that is not mandatory, encodes and publishes it
    *
    * @returns
    *   DeliveryTag for this message, which you should keep until acked or
    *   nacked from the server
    */
  final inline def publish[T: MessageEncoder](
      exchange: ExchangeName,
      routingKey: ShortString,
      payload: T
  )(using NotGiven[T <:< Message[?]]): F[DeliveryTag] =
    publishRaw(exchange, routingKey, MessageRaw.from(payload))

  /** A pipe that publishes [[Envelope]]s that may or may not be mandatory, And
    * returns a delivery tag for every incoming message, and
    * [[ReturnedMessageRaw]] for returned messages.
    *
    * Note that this pipe SHOULD be used exclusively, as it is draining from the
    * returned messages that is backed by a queue.
    *
    * @returns
    *   DeliveryTag for this message, which you should keep until acked or
    *   nacked from the server
    */
  final def publisherRaw(using
      Concurrent[F]
  ): Pipe[F, EnvelopeRaw, DeliveryTag | ReturnedMessageRaw] =
    _.evalMap(publishRaw(_)).mergeHaltBoth(returned)

  /** like [[publisherRaw]], but encodes messages as well
    */
  final def publisher[T: MessageEncoder](using
      Concurrent[F]
  ): Pipe[F, Envelope[T], DeliveryTag | ReturnedMessageRaw] =
    _.map(_.toRaw).through(publisherRaw)

  /** consumes Confirmation messages from the server, this should be used
    * exclusively, or otherwise different instances compete over received
    * values, and each get different set of values.
    */
  def confirmations: Stream[F, Confirmation]

  /** consumes returned messages from the server, this should be used
    * exclusively, or otherwise different instances compete over received
    * values, and each get different set of values.
    *
    * Also note that this is a low level operation that is exposed for special
    * circumstances, always prefer to use publisher pipe instead, unless
    * necessary
    */
  def returned: Stream[F, ReturnedMessageRaw]
}

trait Transaction[F[_]] {

  /** Commits a transaction explicitly, and starts a new transaction
    * immediately.
    */
  def commit: F[Unit]

  /** Rollbacks a transaction explicitly, and starts a new transaction
    * immediately.
    */
  def rollback: F[Unit]
}

trait TransactionalMessaging[F[_]] {

  /** Starts an scope that will commit on success, and rollback otherwise */
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
