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

import lepus.protocol.ChannelClass.*
import lepus.protocol.*
import lepus.protocol.constants.*
import lepus.protocol.domains.*
import lepus.wire.DomainCodecs.*
import scodec.Codec
import scodec.codecs.*

import scala.annotation.switch

object ChannelCodecs {
  private val openCodec: Codec[Open.type] =
    (emptyShortString) ~> provide(Open)
      .withContext("open method")

  private val openOkCodec: Codec[OpenOk.type] =
    (emptyLongString) ~> provide(OpenOk)
      .withContext("openOk method")

  private val flowCodec: Codec[Flow] =
    (reverseByteAligned(bool))
      .as[Flow]
      .withContext("flow method")

  private val flowOkCodec: Codec[FlowOk] =
    (reverseByteAligned(bool))
      .as[FlowOk]
      .withContext("flowOk method")

  private val closeCodec: Codec[Close] =
    (replyCode :: replyText :: classId :: methodId)
      .as[Close]
      .withContext("close method")

  private val closeOkCodec: Codec[CloseOk.type] =
    provide(CloseOk)
      .withContext("closeOk method")

  val all: Codec[ChannelClass] = methodId
    .flatZip(m =>
      ((m: Short): @switch) match {
        case 10 => openCodec.upcast[ChannelClass]
        case 11 => openOkCodec.upcast[ChannelClass]
        case 20 => flowCodec.upcast[ChannelClass]
        case 21 => flowOkCodec.upcast[ChannelClass]
        case 40 => closeCodec.upcast[ChannelClass]
        case 41 => closeOkCodec.upcast[ChannelClass]
      }
    )
    .xmap(_._2, a => (a._methodId, a))
    .withContext("channel methods")
}
