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

import lepus.protocol.TxClass.*
import lepus.protocol.*
import lepus.wire.DomainCodecs.*
import scodec.Codec
import scodec.codecs.*

import scala.annotation.switch

object TxCodecs {
  private val selectCodec: Codec[Select.type] =
    provide(Select)
      .withContext("select method")

  private val selectOkCodec: Codec[SelectOk.type] =
    provide(SelectOk)
      .withContext("selectOk method")

  private val commitCodec: Codec[Commit.type] =
    provide(Commit)
      .withContext("commit method")

  private val commitOkCodec: Codec[CommitOk.type] =
    provide(CommitOk)
      .withContext("commitOk method")

  private val rollbackCodec: Codec[Rollback.type] =
    provide(Rollback)
      .withContext("rollback method")

  private val rollbackOkCodec: Codec[RollbackOk.type] =
    provide(RollbackOk)
      .withContext("rollbackOk method")

  val all: Codec[TxClass] = methodId
    .flatZip(m =>
      ((m: Short): @switch) match {
        case 10 => selectCodec.upcast[TxClass]
        case 11 => selectOkCodec.upcast[TxClass]
        case 20 => commitCodec.upcast[TxClass]
        case 21 => commitOkCodec.upcast[TxClass]
        case 30 => rollbackCodec.upcast[TxClass]
        case 31 => rollbackOkCodec.upcast[TxClass]
      }
    )
    .xmap(_._2, a => (a._methodId, a))
    .withContext("tx methods")
}
