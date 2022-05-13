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

import lepus.protocol.Method
import lepus.protocol.domains.ClassId
import lepus.protocol.classes.*
import lepus.client.codecs.DomainCodecs.classId
import scodec.Codec
import scodec.codecs.discriminated

object MethodCodec {

  val all: Codec[Method] = discriminated[Method]
    .by(classId)
    .subcaseP[ConnectionClass](ClassId(10)) { case m: ConnectionClass => m }(
      ConnectionCodecs.all
    )
    .subcaseP[ChannelClass](ClassId(20)) { case m: ChannelClass => m }(
      ChannelCodecs.all
    )
    .subcaseP[ExchangeClass](ClassId(40)) { case m: ExchangeClass => m }(
      ExchangeCodecs.all
    )
    .subcaseP[QueueClass](ClassId(50)) { case m: QueueClass => m }(
      QueueCodecs.all
    )
    .subcaseP[BasicClass](ClassId(60)) { case m: BasicClass => m }(
      BasicCodecs.all
    )
    .subcaseP[TxClass](ClassId(90)) { case m: TxClass => m }(TxCodecs.all)
    .subcaseP[ConfirmClass](ClassId(85)) { case m: ConfirmClass => m }(
      ConfirmCodecs.all
    )
    .withContext("Method codecs")

}
