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

import lepus.protocol.*
import lepus.protocol.domains.*
import lepus.protocol.*
import lepus.protocol.ChannelClass.*
import lepus.protocol.constants.*
import lepus.wire.DomainCodecs.*
import scodec.{Codec, Encoder, Decoder}
import scodec.codecs.*

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

  val all: Codec[ChannelClass] =
    discriminated[ChannelClass]
      .by(methodId)
      .subcaseP[Open.type](MethodId(10)) { case m: Open.type => m }(openCodec)
      .subcaseP[OpenOk.type](MethodId(11)) { case m: OpenOk.type => m }(
        openOkCodec
      )
      .subcaseP[Flow](MethodId(20)) { case m: Flow => m }(flowCodec)
      .subcaseP[FlowOk](MethodId(21)) { case m: FlowOk => m }(flowOkCodec)
      .subcaseP[Close](MethodId(40)) { case m: Close => m }(closeCodec)
      .subcaseP[CloseOk.type](MethodId(41)) { case m: CloseOk.type => m }(
        closeOkCodec
      )
      .withContext("channel methods")

}
