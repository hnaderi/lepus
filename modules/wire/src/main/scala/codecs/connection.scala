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

import lepus.protocol.ConnectionClass.*
import lepus.protocol.*
import lepus.protocol.constants.*
import lepus.protocol.domains.*
import lepus.wire.DomainCodecs.*
import scodec.Codec
import scodec.codecs.*

import scala.annotation.switch

object ConnectionCodecs {
  private val startCodec: Codec[Start] =
    (byte :: byte :: peerProperties :: longString :: longString)
      .as[Start]
      .withContext("start method")

  private val startOkCodec: Codec[StartOk] =
    (peerProperties :: shortString :: longString :: shortString)
      .as[StartOk]
      .withContext("startOk method")

  private val secureCodec: Codec[Secure] =
    (longString)
      .as[Secure]
      .withContext("secure method")

  private val secureOkCodec: Codec[SecureOk] =
    (longString)
      .as[SecureOk]
      .withContext("secureOk method")

  private val tuneCodec: Codec[Tune] =
    (short16 :: int32 :: short16)
      .as[Tune]
      .withContext("tune method")

  private val tuneOkCodec: Codec[TuneOk] =
    (short16 :: int32 :: short16)
      .as[TuneOk]
      .withContext("tuneOk method")

  private val openCodec: Codec[Open] =
    (path :: emptyShortString :: (reverseByteAligned(bool.unit(false))))
      .as[Open]
      .withContext("open method")

  private val openOkCodec: Codec[OpenOk.type] =
    (emptyShortString) ~> provide(OpenOk)
      .withContext("openOk method")

  private val closeCodec: Codec[Close] =
    (replyCode :: replyText :: classId :: methodId)
      .as[Close]
      .withContext("close method")

  private val closeOkCodec: Codec[CloseOk.type] =
    provide(CloseOk)
      .withContext("closeOk method")

  private val blockedCodec: Codec[Blocked] =
    (shortString)
      .as[Blocked]
      .withContext("blocked method")

  private val unblockedCodec: Codec[Unblocked.type] =
    provide(Unblocked)
      .withContext("unblocked method")

  private val updateSecretCodec: Codec[UpdateSecret] =
    (longString :: shortString)
      .as[UpdateSecret]
      .withContext("updateSecret method")

  private val updateSecretOkCodec: Codec[UpdateSecretOk.type] =
    provide(UpdateSecretOk)
      .withContext("updateSecretOk method")

  val all: Codec[ConnectionClass] = methodId
    .flatZip(m =>
      ((m: Short): @switch) match {
        case 10 => startCodec.upcast[ConnectionClass]
        case 11 => startOkCodec.upcast[ConnectionClass]
        case 20 => secureCodec.upcast[ConnectionClass]
        case 21 => secureOkCodec.upcast[ConnectionClass]
        case 30 => tuneCodec.upcast[ConnectionClass]
        case 31 => tuneOkCodec.upcast[ConnectionClass]
        case 40 => openCodec.upcast[ConnectionClass]
        case 41 => openOkCodec.upcast[ConnectionClass]
        case 50 => closeCodec.upcast[ConnectionClass]
        case 51 => closeOkCodec.upcast[ConnectionClass]
        case 60 => blockedCodec.upcast[ConnectionClass]
        case 61 => unblockedCodec.upcast[ConnectionClass]
        case 70 => updateSecretCodec.upcast[ConnectionClass]
        case 71 => updateSecretOkCodec.upcast[ConnectionClass]
      }
    )
    .xmap(_._2, a => (a._methodId, a))
    .withContext("connection methods")
}
