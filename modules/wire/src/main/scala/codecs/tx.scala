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
import lepus.protocol.TxClass.*
import lepus.protocol.constants.*
import lepus.wire.DomainCodecs.*
import scodec.{Codec, Encoder, Decoder}
import scodec.codecs.*

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

  val all: Codec[TxClass] =
    discriminated[TxClass]
      .by(methodId)
      .subcaseP[Select.type](MethodId(10)) { case m: Select.type => m }(
        selectCodec
      )
      .subcaseP[SelectOk.type](MethodId(11)) { case m: SelectOk.type => m }(
        selectOkCodec
      )
      .subcaseP[Commit.type](MethodId(20)) { case m: Commit.type => m }(
        commitCodec
      )
      .subcaseP[CommitOk.type](MethodId(21)) { case m: CommitOk.type => m }(
        commitOkCodec
      )
      .subcaseP[Rollback.type](MethodId(30)) { case m: Rollback.type => m }(
        rollbackCodec
      )
      .subcaseP[RollbackOk.type](MethodId(31)) { case m: RollbackOk.type => m }(
        rollbackOkCodec
      )
      .withContext("tx methods")

}
