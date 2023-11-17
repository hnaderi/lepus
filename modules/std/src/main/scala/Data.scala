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

package lepus.std

import lepus.protocol.domains.*

/** Pub/Sub topology definition */
final case class TopicDefinition[T](
    exchange: ExchangeName,
    codec: ChannelCodec[T],
    topic: TopicNameEncoder[T]
)

/** RPC topology definition */
final case class RPCDefinition[I, O](
    name: QueueName,
    clientCodec: ChannelCodec[I],
    serverCodec: ChannelCodec[O]
)

/** Work pool topology definition */
final case class WorkPoolDefinition[I](
    name: QueueName,
    codec: ChannelCodec[I]
)
