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

import lepus.protocol.*
import lepus.protocol.domains.*
import org.scalacheck.Arbitrary
import org.scalacheck.Gen

object ConfirmDataGenerator {
  val selectGen: Gen[ConfirmClass.Select] =
    for {
      arg0 <- Arbitrary.arbitrary[NoWait]
    } yield ConfirmClass.Select(arg0)

  given Arbitrary[ConfirmClass.Select] = Arbitrary(selectGen)

  val selectOkGen: Gen[ConfirmClass.SelectOk.type] =
    Gen.const(ConfirmClass.SelectOk)

  given Arbitrary[ConfirmClass.SelectOk.type] = Arbitrary(selectOkGen)

  val classGen: Gen[ConfirmClass] = Gen.oneOf(selectGen, selectOkGen)
  given Arbitrary[ConfirmClass] = Arbitrary(classGen)
}
