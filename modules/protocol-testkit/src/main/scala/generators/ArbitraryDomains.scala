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

package lepus.codecs

import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*
import org.scalacheck.Arbitrary

import DomainGenerators.*

object ArbitraryDomains {
  given Arbitrary[ExchangeName] = Arbitrary(exchangeName)
  given Arbitrary[QueueName] = Arbitrary(queueName)
  given Arbitrary[MethodId] = Arbitrary(methodIds)
  given Arbitrary[ClassId] = Arbitrary(classIds)
  given Arbitrary[ShortString] = Arbitrary(shortString)
  given Arbitrary[LongString] = Arbitrary(longString)
  given Arbitrary[Timestamp] = Arbitrary(timestamp)
  given Arbitrary[ConsumerTag] = Arbitrary(consumerTag)
  given Arbitrary[MessageCount] = Arbitrary(messageCount)
  given Arbitrary[Path] = Arbitrary(path)
  given Arbitrary[DeliveryTag] = Arbitrary(deliveryTag)
  given Arbitrary[DeliveryMode] = Arbitrary(deliveryMode)
  given Arbitrary[Priority] = Arbitrary(priority)
  given Arbitrary[Decimal] = Arbitrary(decimal)
  given Arbitrary[FieldData] = Arbitrary(fieldData)
  given Arbitrary[FieldTable] = Arbitrary(fieldTable)
  given Arbitrary[ReplyCode] = Arbitrary(replyCode)
}
