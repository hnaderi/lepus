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

enum QueueClass(methodId: MethodId, synchronous: Boolean)
    extends Class(ClassId(50))
    with Method(methodId, synchronous) {

  case Declare(
      queue: QueueName,
      passive: Boolean,
      durable: Boolean,
      exclusive: Boolean,
      autoDelete: Boolean,
      noWait: NoWait,
      arguments: FieldTable
  ) extends QueueClass(MethodId(10), true) with Request

  case DeclareOk(
      queue: QueueName,
      messageCount: MessageCount,
      consumerCount: Int
  ) extends QueueClass(MethodId(11), true) with Response

  case Bind(
      queue: QueueName,
      exchange: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait,
      arguments: FieldTable
  ) extends QueueClass(MethodId(20), true) with Request

  case BindOk extends QueueClass(MethodId(21), true) with Response

  case Unbind(
      queue: QueueName,
      exchange: ExchangeName,
      routingKey: ShortString,
      arguments: FieldTable
  ) extends QueueClass(MethodId(50), true) with Request

  case UnbindOk extends QueueClass(MethodId(51), true) with Response

  case Purge(queue: QueueName, noWait: NoWait)
      extends QueueClass(MethodId(30), true)
      with Request

  case PurgeOk(messageCount: MessageCount)
      extends QueueClass(MethodId(31), true)
      with Response

  case Delete(
      queue: QueueName,
      ifUnused: Boolean,
      ifEmpty: Boolean,
      noWait: NoWait
  ) extends QueueClass(MethodId(40), true) with Request

  case DeleteOk(messageCount: MessageCount)
      extends QueueClass(MethodId(41), true)
      with Response

}
