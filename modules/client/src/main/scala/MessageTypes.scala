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

import fs2.concurrent.Signal
import lepus.protocol.classes.basic.Properties
import lepus.protocol.constants.*
import lepus.protocol.domains.*
import scodec.bits.ByteVector

final case class Envelope(
    exchange: ExchangeName,
    routingKey: ShortString,
    mandatory: Boolean,
    // immediate: Boolean, // RabbitMQ does not implement immediate flag
    message: Message
)

final case class Message(
    payload: ByteVector,
    properties: Properties = Properties()
)

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
