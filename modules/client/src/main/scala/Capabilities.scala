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

import lepus.protocol.domains.FieldTable
import lepus.protocol.domains.ShortString

final case class Capabilities(
    publisherConfirm: Boolean = false,
    directReply: Boolean = false,
    consumerPriority: Boolean = false,
    authenticationFailure: Boolean = false,
    consumerCancelNotify: Boolean = false,
    basicNack: Boolean = false,
    e2eBinding: Boolean = false,
    perConsumerQos: Boolean = false,
    connectionBlocked: Boolean = false
)

object Capabilities {
  val none: Capabilities = Capabilities()

  extension (ft: FieldTable) {
    private inline def supports(key: String): Boolean =
      ft.get(ShortString(key)).contains(true)
  }
  extension (caps: Capabilities) {
    def toFieldTable: FieldTable = FieldTable(
      ShortString("publisher_confirms") -> caps.publisherConfirm,
      ShortString("direct_reply_to") -> caps.directReply,
      ShortString("consumer_priorities") -> caps.consumerPriority,
      ShortString("authentication_failure_close") -> caps.authenticationFailure,
      ShortString("consumer_cancel_notify") -> caps.consumerCancelNotify,
      ShortString("basic.nack") -> caps.basicNack,
      ShortString("exchange_exchange_bindings") -> caps.e2eBinding,
      ShortString("per_consumer_qos") -> caps.perConsumerQos,
      ShortString("connection.blocked") -> caps.connectionBlocked
    )
  }

  def from(ft: FieldTable): Capabilities = Capabilities(
    publisherConfirm = ft.supports("publisher_confirms"),
    directReply = ft.supports("direct_reply_to"),
    consumerPriority = ft.supports("consumer_priorities"),
    authenticationFailure = ft.supports("authentication_failure_close"),
    consumerCancelNotify = ft.supports("consumer_cancel_notify"),
    basicNack = ft.supports("basic.nack"),
    e2eBinding = ft.supports("exchange_exchange_bindings"),
    perConsumerQos = ft.supports("per_consumer_qos"),
    connectionBlocked = ft.supports("connection.blocked")
  )
}
