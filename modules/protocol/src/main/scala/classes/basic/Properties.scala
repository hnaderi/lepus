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

package lepus.protocol.classes.basic

import lepus.protocol.domains.*
import lepus.protocol.constants.*

final case class Properties(
    contentType: Option[ShortString] = None,
    contentEncoding: Option[ShortString] = None,
    headers: Option[FieldTable] = None,
    deliveryMode: Option[DeliveryMode] = None,
    priority: Option[Priority] = None,
    correlationId: Option[ShortString] = None,
    replyTo: Option[ShortString] = None,
    expiration: Option[ShortString] = None,
    messageId: Option[ShortString] = None,
    timestamp: Option[Timestamp] = None,
    msgType: Option[ShortString] = None,
    userId: Option[ShortString] = None,
    appId: Option[ShortString] = None,
    clusterId: Option[ShortString] = None
)
