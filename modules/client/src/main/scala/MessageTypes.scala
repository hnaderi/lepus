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

import lepus.protocol.classes.basic.Properties
import lepus.protocol.constants.*
import lepus.protocol.domains.*
import scodec.bits.ByteVector

final case class Envelope[T](
    exchange: ExchangeName,
    routingKey: ShortString,
    mandatory: Boolean,
    // immediate: Boolean, // RabbitMQ does not implement immediate flag
    message: Message[T]
)
object Envelope {
  extension [T](msg: Envelope[T])(using enc: EnvelopeEncoder[T]) {
    def toRaw: EnvelopeRaw = enc.encode(msg)
  }
  extension (msg: EnvelopeRaw) {
    def decodeTo[T: EnvelopeDecoder]: Either[Throwable, Envelope[T]] =
      EnvelopeDecoder[T].decode(msg)
  }
}
final case class Message[T](
    payload: T,
    properties: Properties = Properties()
) {
  def withPayload[A](value: A): Message[A] = copy(payload = value)
  def withProperties(value: Properties): Message[T] = copy(properties = value)

  def withContentType(value: ShortString) =
    withProperties(properties.withContentType(value))

  def withContentEncoding(value: ShortString): Message[T] = withProperties(
    properties.withContentEncoding(value)
  )
  def withHeaders(value: FieldTable): Message[T] = withProperties(
    properties.withHeaders(value)
  )
  def withDeliveryMode(value: DeliveryMode): Message[T] = withProperties(
    properties.withDeliveryMode(value)
  )
  def withPriority(value: Priority): Message[T] = withProperties(
    properties.withPriority(value)
  )
  def withCorrelationId(value: ShortString): Message[T] = withProperties(
    properties.withCorrelationId(value)
  )
  def withReplyTo(value: ShortString): Message[T] = withProperties(
    properties.withReplyTo(value)
  )
  def withExpiration(value: ShortString): Message[T] = withProperties(
    properties.withExpiration(value)
  )
  def withMessageId(value: ShortString): Message[T] = withProperties(
    properties.withMessageId(value)
  )
  def withTimestamp(value: Timestamp): Message[T] = withProperties(
    properties.withTimestamp(value)
  )
  def withMsgType(value: ShortString): Message[T] = withProperties(
    properties.withMsgType(value)
  )
  def withUserId(value: ShortString): Message[T] = withProperties(
    properties.withUserId(value)
  )
  def withAppId(value: ShortString): Message[T] = withProperties(
    properties.withAppId(value)
  )
  def withClusterId(value: ShortString): Message[T] = withProperties(
    properties.withClusterId(value)
  )
}
object Message {
  extension [T](msg: Message[T])(using enc: EnvelopeEncoder[T]) {
    def toRaw: MessageRaw = enc.encode(msg)
  }
  extension (msg: MessageRaw) {
    def decodeTo[T: EnvelopeDecoder]: Either[Throwable, Message[T]] =
      EnvelopeDecoder[T].decode(msg)
  }
}

type EnvelopeRaw = Envelope[ByteVector]
val EnvelopeRaw = Envelope
type MessageRaw = Message[ByteVector]
object MessageRaw {
  def apply(
      payload: ByteVector,
      properties: Properties = Properties.empty
  ): MessageRaw = Message(payload, properties)
  def from[T](payload: T, properties: Properties = Properties.empty)(using
      enc: EnvelopeEncoder[T]
  ): MessageRaw = enc.encode(Message(payload, properties))
}

type AsyncContent = ReturnedMessageRaw | DeliveredMessageRaw

final case class ReturnedMessage[T](
    replyCode: ReplyCode,
    replyText: ReplyText,
    exchange: ExchangeName,
    routingKey: ShortString,
    message: Message[T]
)
type ReturnedMessageRaw = ReturnedMessage[ByteVector]
val ReturnedMessageRaw = ReturnedMessage

final case class DeliveredMessage[T](
    consumerTag: ConsumerTag,
    deliveryTag: DeliveryTag,
    redelivered: Redelivered,
    exchange: ExchangeName,
    routingKey: ShortString,
    message: Message[T]
)
type DeliveredMessageRaw = DeliveredMessage[ByteVector]
val DeliveredMessageRaw = DeliveredMessage

final case class SynchronousGet[T](
    deliveryTag: DeliveryTag,
    redelivered: Redelivered,
    exchange: ExchangeName,
    routingKey: ShortString,
    messageCount: MessageCount,
    message: Message[T]
)
type SynchronousGetRaw = SynchronousGet[ByteVector]
val SynchronousGetRaw = SynchronousGet

enum Acknowledgment {
  case Ack, Nack
}
final case class Confirmation(
    kind: Acknowledgment,
    tag: DeliveryTag,
    multiple: Boolean
)

enum ConsumeMode {
  case RaiseOnError(ack: Boolean)
  case NackOnError
}
