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

import lepus.client.EnvelopeCodec
import lepus.protocol.domains.*

final case class TopicDefinition[T](
    exchange: ExchangeName,
    codec: ChannelCodec[T],
    topic: TopicNameEncoder[T]
)

final case class EndpointDefinition[I, O](
    name: QueueName,
    clientCodec: ChannelCodec[I],
    serverCodec: ChannelCodec[O]
)

final case class WorkPoolDefinition[I](
    name: QueueName,
    codec: ChannelCodec[I]
)
