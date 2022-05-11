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

package lepus.client.codecs

import lepus.protocol.*
import lepus.protocol.domains.*
import lepus.protocol.classes.*
import lepus.protocol.classes.ConnectionClass.*
import lepus.protocol.constants.*
import lepus.client.codecs.DomainCodecs.*
import scodec.{Codec, Encoder, Decoder}
import scodec.codecs.*

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
    (path :: emptyShortString :: (reverseByteAligned(7, bool.unit(false))))
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

  val all: Codec[ConnectionClass] =
    discriminated[ConnectionClass]
      .by(methodId)
      .subcaseP[Start](MethodId(10)) { case m: Start => m }(startCodec)
      .subcaseP[StartOk](MethodId(11)) { case m: StartOk => m }(startOkCodec)
      .subcaseP[Secure](MethodId(20)) { case m: Secure => m }(secureCodec)
      .subcaseP[SecureOk](MethodId(21)) { case m: SecureOk => m }(secureOkCodec)
      .subcaseP[Tune](MethodId(30)) { case m: Tune => m }(tuneCodec)
      .subcaseP[TuneOk](MethodId(31)) { case m: TuneOk => m }(tuneOkCodec)
      .subcaseP[Open](MethodId(40)) { case m: Open => m }(openCodec)
      .subcaseP[OpenOk.type](MethodId(41)) { case m: OpenOk.type => m }(
        openOkCodec
      )
      .subcaseP[Close](MethodId(50)) { case m: Close => m }(closeCodec)
      .subcaseP[CloseOk.type](MethodId(51)) { case m: CloseOk.type => m }(
        closeOkCodec
      )
      .subcaseP[Blocked](MethodId(60)) { case m: Blocked => m }(blockedCodec)
      .subcaseP[Unblocked.type](MethodId(61)) { case m: Unblocked.type => m }(
        unblockedCodec
      )
      .subcaseP[UpdateSecret](MethodId(70)) { case m: UpdateSecret => m }(
        updateSecretCodec
      )
      .subcaseP[UpdateSecretOk.type](MethodId(71)) {
        case m: UpdateSecretOk.type => m
      }(updateSecretOkCodec)
      .withContext("connection methods")

}
