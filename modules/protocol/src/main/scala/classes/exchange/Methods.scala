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

enum ExchangeClass(methodId: MethodId, synchronous: Boolean)
    extends Class(ClassId(40))
    with Method(methodId, synchronous) {

  case Declare(
      exchange: ExchangeName,
      `type`: ShortString,
      passive: Boolean,
      durable: Boolean,
      autoDelete: Boolean,
      internal: Boolean,
      noWait: NoWait,
      arguments: FieldTable
  ) extends ExchangeClass(MethodId(10), true) with Request

  case DeclareOk extends ExchangeClass(MethodId(11), true) with Response

  case Delete(exchange: ExchangeName, ifUnused: Boolean, noWait: NoWait)
      extends ExchangeClass(MethodId(20), true)
      with Request

  case DeleteOk extends ExchangeClass(MethodId(21), true) with Response

  case Bind(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait,
      arguments: FieldTable
  ) extends ExchangeClass(MethodId(30), true) with Request

  case BindOk extends ExchangeClass(MethodId(31), true) with Response

  case Unbind(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait,
      arguments: FieldTable
  ) extends ExchangeClass(MethodId(40), true) with Request

  case UnbindOk extends ExchangeClass(MethodId(51), true) with Response

}
