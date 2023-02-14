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

final case class Envelope0[T](
    exchange: ExchangeName,
    routingKey: ShortString,
    mandatory: Boolean,
    // immediate: Boolean, // RabbitMQ does not implement immediate flag
    message: Message0[T]
)
final case class Message0[T](
    payload: T,
    properties: Properties = Properties()
) {
  def withPayload[A](value: A): Message0[A] = copy(payload = value)
  def withProperties(value: Properties): Message0[T] = copy(properties = value)

  def withContentType(value: ShortString) =
    withProperties(properties.withContentType(value))

  def withContentEncoding(value: ShortString): Message0[T] = withProperties(
    properties.withContentEncoding(value)
  )
  def withHeaders(value: FieldTable): Message0[T] = withProperties(
    properties.withHeaders(value)
  )
  def withDeliveryMode(value: DeliveryMode): Message0[T] = withProperties(
    properties.withDeliveryMode(value)
  )
  def withPriority(value: Priority): Message0[T] = withProperties(
    properties.withPriority(value)
  )
  def withCorrelationId(value: ShortString): Message0[T] = withProperties(
    properties.withCorrelationId(value)
  )
  def withReplyTo(value: ShortString): Message0[T] = withProperties(
    properties.withReplyTo(value)
  )
  def withExpiration(value: ShortString): Message0[T] = withProperties(
    properties.withExpiration(value)
  )
  def withMessageId(value: ShortString): Message0[T] = withProperties(
    properties.withMessageId(value)
  )
  def withTimestamp(value: Timestamp): Message0[T] = withProperties(
    properties.withTimestamp(value)
  )
  def withMsgType(value: ShortString): Message0[T] = withProperties(
    properties.withMsgType(value)
  )
  def withUserId(value: ShortString): Message0[T] = withProperties(
    properties.withUserId(value)
  )
  def withAppId(value: ShortString): Message0[T] = withProperties(
    properties.withAppId(value)
  )
  def withClusterId(value: ShortString): Message0[T] = withProperties(
    properties.withClusterId(value)
  )
}

type Envelope = Envelope0[ByteVector]
val Envelope = Envelope0
type Message = Message0[ByteVector]
val Message = Message0

type AsyncContent = ReturnedMessage | DeliveredMessage

final case class ReturnedMessage(
    replyCode: ReplyCode,
    replyText: ReplyText,
    exchange: ExchangeName,
    routingKey: ShortString,
    message: Message
)

final case class DeliveredMessage(
    consumerTag: ConsumerTag,
    deliveryTag: DeliveryTag,
    redelivered: Redelivered,
    exchange: ExchangeName,
    routingKey: ShortString,
    message: Message
)

final case class SynchronousGet(
    deliveryTag: DeliveryTag,
    redelivered: Redelivered,
    exchange: ExchangeName,
    routingKey: ShortString,
    messageCount: MessageCount,
    message: Message
)

enum Acknowledgment {
  case Ack, Nack
}
final case class Confirmation(
    kind: Acknowledgment,
    tag: DeliveryTag,
    multiple: Boolean
)
