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

import cats.implicits.*
import lepus.protocol.*
import lepus.protocol.classes.basic.Properties
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*
import munit.FunSuite
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Prop._

import scala.annotation.meta.field

object DomainGenerators {
  extension [A](g: Gen[A]) {
    def emap[B](f: A => Either[String, B]): Gen[B] =
      g.flatMap(a => f(a).map(Gen.const).getOrElse(Gen.fail))
  }
  val channelNumber: Gen[ChannelNumber] =
    Arbitrary.arbitrary[Short].map(ChannelNumber(_))

  val exchangeName: Gen[ExchangeName] = Gen.alphaNumStr.emap(ExchangeName(_))
  val queueName: Gen[QueueName] = Gen.alphaNumStr.emap(QueueName(_))

  val methodIds: Gen[MethodId] = Arbitrary.arbitrary[Short].map(MethodId(_))
  val classIds: Gen[ClassId] = Arbitrary.arbitrary[Short].map(ClassId(_))

  val shortString: Gen[ShortString] = Gen
    .choose(0, 255)
    .flatMap(n => Gen.stringOfN(n, Gen.alphaNumChar).emap(ShortString(_)))

  val longString: Gen[LongString] =
    Gen.alphaNumStr.emap(LongString(_))

  val timestamp: Gen[Timestamp] = Arbitrary.arbitrary[Long].map(Timestamp(_))

  val messageCount: Gen[MessageCount] = Gen.posNum[Long].emap(MessageCount(_))
  val consumerTag: Gen[ConsumerTag] = shortString.map(ConsumerTag(_))
  val path: Gen[Path] = shortString.emap(Path(_))
  val deliveryTag: Gen[DeliveryTag] =
    Arbitrary.arbitrary[Long].map(DeliveryTag(_))

  val deliveryMode: Gen[DeliveryMode] =
    Gen.oneOf(DeliveryMode.Persistent, DeliveryMode.NonPersistent)
  val priority: Gen[Priority] = Gen.oneOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

  val decimal: Gen[Decimal] = for {
    scale <- Arbitrary.arbitrary[Byte]
    value <- Arbitrary.arbitrary[Int]
  } yield Decimal(scale, value)

  def fieldData: Gen[FieldData] = Gen.oneOf(
    shortString,
    longString,
    timestamp,
    decimal,
    Arbitrary.arbitrary[Boolean],
    Arbitrary.arbitrary[Byte],
    Arbitrary.arbitrary[Short],
    Arbitrary.arbitrary[Int],
    Arbitrary.arbitrary[Long],
    Arbitrary.arbitrary[Float],
    Arbitrary.arbitrary[Double]
  )

  private val fieldElem = for {
    k <- shortString
    v <- fieldData
  } yield (k, v)

  private val fieldMap = Gen.mapOf(fieldElem)

  def fieldTable: Gen[FieldTable] =
    Gen.recursive[FieldTable](ft =>
      Gen.choose(1, 7).flatMap { n =>
        if n > 1 then
          for {
            k <- shortString
            v <- fieldMap
            nest = FieldTable(v)
            parent <- ft
          } yield parent.copy(values = parent.values.updated(k, nest))
        else fieldMap.map(FieldTable(_))
      }
    )

  val replyCode: Gen[ReplyCode] = Gen.oneOf(ReplyCode.values.toSeq)

  val properties: Gen[Properties] = for {
    contentType <- Gen.option(shortString)
    contentEncoding <- Gen.option(shortString)
    headers <- Gen.option(fieldTable)
    deliveryModeArg <- Gen.option(deliveryMode)
    priorityArg <- Gen.option[Priority](priority)
    correlationId <- Gen.option(shortString)
    replyTo <- Gen.option(shortString)
    expiration <- Gen.option(shortString)
    messageId <- Gen.option(shortString)
    timestamp <- Gen.option(timestamp)
    msgType <- Gen.option(shortString)
    userId <- Gen.option(shortString)
    appId <- Gen.option(shortString)
    clusterId <- Gen.option(shortString)
  } yield Properties(
    contentType,
    contentEncoding,
    headers,
    deliveryModeArg,
    priorityArg,
    correlationId,
    replyTo,
    expiration,
    messageId,
    timestamp,
    msgType,
    userId,
    appId,
    clusterId
  )
}
