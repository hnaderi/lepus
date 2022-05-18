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

import lepus.protocol.QueueClass.*
import lepus.protocol.*
import lepus.protocol.constants.*
import lepus.protocol.domains.*
import lepus.wire.DomainCodecs.*
import scodec.Codec
import scodec.codecs.*

import scala.annotation.switch

object QueueCodecs {
  private val declareCodec: Codec[Declare] =
    ((short16.unit(0) :: queueName) ++ (reverseByteAligned(
      bool :: bool :: bool :: bool :: noWait
    ) :+ (fieldTable)))
      .as[Declare]
      .withContext("declare method")

  private val declareOkCodec: Codec[DeclareOk] =
    (queueName :: messageCount :: int32)
      .as[DeclareOk]
      .withContext("declareOk method")

  private val bindCodec: Codec[Bind] =
    ((short16.unit(
      0
    ) :: queueName :: exchangeName :: shortString) ++ (reverseByteAligned(
      noWait
    ) :: (fieldTable)))
      .as[Bind]
      .withContext("bind method")

  private val bindOkCodec: Codec[BindOk.type] =
    provide(BindOk)
      .withContext("bindOk method")

  private val unbindCodec: Codec[Unbind] =
    (short16.unit(0) :: queueName :: exchangeName :: shortString :: fieldTable)
      .as[Unbind]
      .withContext("unbind method")

  private val unbindOkCodec: Codec[UnbindOk.type] =
    provide(UnbindOk)
      .withContext("unbindOk method")

  private val purgeCodec: Codec[Purge] =
    (short16.unit(0) :: queueName :: (reverseByteAligned(noWait)))
      .as[Purge]
      .withContext("purge method")

  private val purgeOkCodec: Codec[PurgeOk] =
    (messageCount)
      .as[PurgeOk]
      .withContext("purgeOk method")

  private val deleteCodec: Codec[Delete] =
    ((short16.unit(0) :: queueName) ++ (reverseByteAligned(
      bool :: bool :: noWait
    )))
      .as[Delete]
      .withContext("delete method")

  private val deleteOkCodec: Codec[DeleteOk] =
    (messageCount)
      .as[DeleteOk]
      .withContext("deleteOk method")

  val all: Codec[QueueClass] = methodId
    .flatZip(m =>
      ((m: Short): @switch) match {
        case 10 => declareCodec.upcast[QueueClass]
        case 11 => declareOkCodec.upcast[QueueClass]
        case 20 => bindCodec.upcast[QueueClass]
        case 21 => bindOkCodec.upcast[QueueClass]
        case 50 => unbindCodec.upcast[QueueClass]
        case 51 => unbindOkCodec.upcast[QueueClass]
        case 30 => purgeCodec.upcast[QueueClass]
        case 31 => purgeOkCodec.upcast[QueueClass]
        case 40 => deleteCodec.upcast[QueueClass]
        case 41 => deleteOkCodec.upcast[QueueClass]
      }
    )
    .xmap(_._2, a => (a._methodId, a))
    .withContext("queue methods")
}
