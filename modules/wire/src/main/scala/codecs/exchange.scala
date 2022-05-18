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

import lepus.protocol.ExchangeClass.*
import lepus.protocol.*
import lepus.protocol.constants.*
import lepus.protocol.domains.*
import lepus.wire.DomainCodecs.*
import scodec.Codec
import scodec.codecs.*

import scala.annotation.switch

object ExchangeCodecs {
  private val declareCodec: Codec[Declare] =
    ((short16.unit(0) :: exchangeName :: shortString) ++ (reverseByteAligned(
      bool :: bool :: bool :: bool :: noWait
    ) :+ (fieldTable)))
      .as[Declare]
      .withContext("declare method")

  private val declareOkCodec: Codec[DeclareOk.type] =
    provide(DeclareOk)
      .withContext("declareOk method")

  private val deleteCodec: Codec[Delete] =
    ((short16.unit(0) :: exchangeName) ++ (reverseByteAligned(bool :: noWait)))
      .as[Delete]
      .withContext("delete method")

  private val deleteOkCodec: Codec[DeleteOk.type] =
    provide(DeleteOk)
      .withContext("deleteOk method")

  private val bindCodec: Codec[Bind] =
    ((short16.unit(
      0
    ) :: exchangeName :: exchangeName :: shortString) ++ (reverseByteAligned(
      noWait
    ) :: (fieldTable)))
      .as[Bind]
      .withContext("bind method")

  private val bindOkCodec: Codec[BindOk.type] =
    provide(BindOk)
      .withContext("bindOk method")

  private val unbindCodec: Codec[Unbind] =
    ((short16.unit(
      0
    ) :: exchangeName :: exchangeName :: shortString) ++ (reverseByteAligned(
      noWait
    ) :: (fieldTable)))
      .as[Unbind]
      .withContext("unbind method")

  private val unbindOkCodec: Codec[UnbindOk.type] =
    provide(UnbindOk)
      .withContext("unbindOk method")

  val all: Codec[ExchangeClass] = methodId
    .flatZip(m =>
      ((m: Short): @switch) match {
        case 10 => declareCodec.upcast[ExchangeClass]
        case 11 => declareOkCodec.upcast[ExchangeClass]
        case 20 => deleteCodec.upcast[ExchangeClass]
        case 21 => deleteOkCodec.upcast[ExchangeClass]
        case 30 => bindCodec.upcast[ExchangeClass]
        case 31 => bindOkCodec.upcast[ExchangeClass]
        case 40 => unbindCodec.upcast[ExchangeClass]
        case 51 => unbindOkCodec.upcast[ExchangeClass]
      }
    )
    .xmap(_._2, a => (a._methodId, a))
    .withContext("exchange methods")
}
