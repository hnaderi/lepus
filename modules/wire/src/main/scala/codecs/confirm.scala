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
import lepus.protocol.ConfirmClass.*
import lepus.protocol.constants.*
import lepus.wire.DomainCodecs.*
import scodec.{Codec, Encoder, Decoder}
import scodec.codecs.*

object ConfirmCodecs {

  private val selectCodec: Codec[Select] =
    (reverseByteAligned(noWait))
      .as[Select]
      .withContext("select method")

  private val selectOkCodec: Codec[SelectOk.type] =
    provide(SelectOk)
      .withContext("selectOk method")

  val all: Codec[ConfirmClass] =
    discriminated[ConfirmClass]
      .by(methodId)
      .subcaseP[Select](MethodId(10)) { case m: Select => m }(selectCodec)
      .subcaseP[SelectOk.type](MethodId(11)) { case m: SelectOk.type => m }(
        selectOkCodec
      )
      .withContext("confirm methods")

}
