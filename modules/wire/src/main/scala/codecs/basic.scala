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

package lepus.wire

import lepus.protocol.BasicClass.*
import lepus.protocol.*
import lepus.protocol.constants.*
import lepus.protocol.domains.*
import lepus.wire.DomainCodecs.*
import scodec.Codec
import scodec.codecs.*

import scala.annotation.switch

object BasicCodecs {
  private val qosCodec: Codec[Qos] =
    (int32 :: short16 :: (reverseByteAligned(bool)))
      .as[Qos]
      .withContext("qos method")

  private val qosOkCodec: Codec[QosOk.type] =
    provide(QosOk)
      .withContext("qosOk method")

  private val consumeCodec: Codec[Consume] =
    ((short16.unit(0) :: queueName :: consumerTag) ++ (reverseByteAligned(
      noLocal :: noAck :: bool :: noWait
    ) :+ (fieldTable)))
      .as[Consume]
      .withContext("consume method")

  private val consumeOkCodec: Codec[ConsumeOk] =
    (consumerTag)
      .as[ConsumeOk]
      .withContext("consumeOk method")

  private val cancelCodec: Codec[Cancel] =
    (consumerTag :: (reverseByteAligned(noWait)))
      .as[Cancel]
      .withContext("cancel method")

  private val cancelOkCodec: Codec[CancelOk] =
    (consumerTag)
      .as[CancelOk]
      .withContext("cancelOk method")

  private val publishCodec: Codec[Publish] =
    ((short16.unit(0) :: exchangeName :: shortString) ++ (reverseByteAligned(
      bool :: bool
    )))
      .as[Publish]
      .withContext("publish method")

  private val returnCodec: Codec[Return] =
    (replyCode :: replyText :: exchangeName :: shortString)
      .as[Return]
      .withContext("return method")

  private val deliverCodec: Codec[Deliver] =
    ((consumerTag :: deliveryTag) ++ (reverseByteAligned(
      redelivered
    ) :: (exchangeName :: shortString)))
      .as[Deliver]
      .withContext("deliver method")

  private val getCodec: Codec[Get] =
    (short16.unit(0) :: queueName :: (reverseByteAligned(noAck)))
      .as[Get]
      .withContext("get method")

  private val getOkCodec: Codec[GetOk] =
    (deliveryTag :: (reverseByteAligned(
      redelivered
    ) :: (exchangeName :: shortString :: messageCount)))
      .as[GetOk]
      .withContext("getOk method")

  private val getEmptyCodec: Codec[GetEmpty.type] =
    (emptyShortString) ~> provide(GetEmpty)
      .withContext("getEmpty method")

  private val ackCodec: Codec[Ack] =
    (deliveryTag :: (reverseByteAligned(bool)))
      .as[Ack]
      .withContext("ack method")

  private val rejectCodec: Codec[Reject] =
    (deliveryTag :: (reverseByteAligned(bool)))
      .as[Reject]
      .withContext("reject method")

  private val recoverAsyncCodec: Codec[RecoverAsync] =
    (reverseByteAligned(bool))
      .as[RecoverAsync]
      .withContext("recoverAsync method")

  private val recoverCodec: Codec[Recover] =
    (reverseByteAligned(bool))
      .as[Recover]
      .withContext("recover method")

  private val recoverOkCodec: Codec[RecoverOk.type] =
    provide(RecoverOk)
      .withContext("recoverOk method")

  private val nackCodec: Codec[Nack] =
    (deliveryTag :: (reverseByteAligned(bool :: bool)))
      .as[Nack]
      .withContext("nack method")

  val all: Codec[BasicClass] = methodId
    .flatZip(m =>
      ((m: Short): @switch) match {
        case 10  => qosCodec.upcast[BasicClass]
        case 11  => qosOkCodec.upcast[BasicClass]
        case 20  => consumeCodec.upcast[BasicClass]
        case 21  => consumeOkCodec.upcast[BasicClass]
        case 30  => cancelCodec.upcast[BasicClass]
        case 31  => cancelOkCodec.upcast[BasicClass]
        case 40  => publishCodec.upcast[BasicClass]
        case 50  => returnCodec.upcast[BasicClass]
        case 60  => deliverCodec.upcast[BasicClass]
        case 70  => getCodec.upcast[BasicClass]
        case 71  => getOkCodec.upcast[BasicClass]
        case 72  => getEmptyCodec.upcast[BasicClass]
        case 80  => ackCodec.upcast[BasicClass]
        case 90  => rejectCodec.upcast[BasicClass]
        case 100 => recoverAsyncCodec.upcast[BasicClass]
        case 110 => recoverCodec.upcast[BasicClass]
        case 111 => recoverOkCodec.upcast[BasicClass]
        case 120 => nackCodec.upcast[BasicClass]
      }
    )
    .xmap(_._2, a => (a._methodId, a))
    .withContext("basic methods")
}
