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

package lepus.protocol.classes

import lepus.protocol.*
import lepus.protocol.domains.*
import lepus.protocol.constants.*

enum BasicClass(methodId: MethodId)
    extends Class(ClassId(60))
    with Method(methodId) {

  case Qos(prefetchSize: Int, prefetchCount: Short, global: Boolean)
      extends BasicClass(MethodId(10))
      with Request

  case QosOk extends BasicClass(MethodId(11)) with Response

  case Consume(
      queue: QueueName,
      consumerTag: ConsumerTag,
      noLocal: NoLocal,
      noAck: NoAck,
      exclusive: Boolean,
      noWait: NoWait,
      arguments: FieldTable
  ) extends BasicClass(MethodId(20)) with Request

  case ConsumeOk(consumerTag: ConsumerTag)
      extends BasicClass(MethodId(21))
      with Response

  case Cancel(consumerTag: ConsumerTag, noWait: NoWait)
      extends BasicClass(MethodId(30))
      with Request
      with Response

  case CancelOk(consumerTag: ConsumerTag)
      extends BasicClass(MethodId(31))
      with Request
      with Response

  case Publish(
      exchange: ExchangeName,
      routingKey: ShortString,
      mandatory: Boolean,
      immediate: Boolean
  ) extends BasicClass(MethodId(40)) with Request

  case Return(
      replyCode: ReplyCode,
      replyText: ReplyText,
      exchange: ExchangeName,
      routingKey: ShortString
  ) extends BasicClass(MethodId(50)) with Response

  case Deliver(
      consumerTag: ConsumerTag,
      deliveryTag: DeliveryTag,
      redelivered: Redelivered,
      exchange: ExchangeName,
      routingKey: ShortString
  ) extends BasicClass(MethodId(60)) with Response

  case Get(queue: QueueName, noAck: NoAck)
      extends BasicClass(MethodId(70))
      with Request

  case GetOk(
      deliveryTag: DeliveryTag,
      redelivered: Redelivered,
      exchange: ExchangeName,
      routingKey: ShortString,
      messageCount: MessageCount
  ) extends BasicClass(MethodId(71)) with Response

  case GetEmpty extends BasicClass(MethodId(72)) with Response

  case Ack(deliveryTag: DeliveryTag, multiple: Boolean)
      extends BasicClass(MethodId(80))
      with Request
      with Response

  case Reject(deliveryTag: DeliveryTag, requeue: Boolean)
      extends BasicClass(MethodId(90))
      with Request

  case RecoverAsync(requeue: Boolean)
      extends BasicClass(MethodId(100))
      with Request

  case Recover(requeue: Boolean) extends BasicClass(MethodId(110)) with Request

  case RecoverOk extends BasicClass(MethodId(111)) with Response

  case Nack(deliveryTag: DeliveryTag, multiple: Boolean, requeue: Boolean)
      extends BasicClass(MethodId(120))
      with Request
      with Response

}
