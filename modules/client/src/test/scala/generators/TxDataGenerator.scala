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

package lepus.codecs

import lepus.protocol.Method
import lepus.protocol.classes.*
import lepus.protocol.constants.*
import lepus.protocol.domains.*
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import ArbitraryDomains.given

object TxDataGenerator {

  val selectGen: Gen[TxClass.Select.type] =
    Gen.const(TxClass.Select)

  given Arbitrary[TxClass.Select.type] = Arbitrary(selectGen)

  val selectOkGen: Gen[TxClass.SelectOk.type] =
    Gen.const(TxClass.SelectOk)

  given Arbitrary[TxClass.SelectOk.type] = Arbitrary(selectOkGen)

  val commitGen: Gen[TxClass.Commit.type] =
    Gen.const(TxClass.Commit)

  given Arbitrary[TxClass.Commit.type] = Arbitrary(commitGen)

  val commitOkGen: Gen[TxClass.CommitOk.type] =
    Gen.const(TxClass.CommitOk)

  given Arbitrary[TxClass.CommitOk.type] = Arbitrary(commitOkGen)

  val rollbackGen: Gen[TxClass.Rollback.type] =
    Gen.const(TxClass.Rollback)

  given Arbitrary[TxClass.Rollback.type] = Arbitrary(rollbackGen)

  val rollbackOkGen: Gen[TxClass.RollbackOk.type] =
    Gen.const(TxClass.RollbackOk)

  given Arbitrary[TxClass.RollbackOk.type] = Arbitrary(rollbackOkGen)

  val classGen: Gen[TxClass] = Gen.oneOf(
    selectGen,
    selectOkGen,
    commitGen,
    commitOkGen,
    rollbackGen,
    rollbackOkGen
  )

  given Arbitrary[TxClass] = Arbitrary(classGen)

}
