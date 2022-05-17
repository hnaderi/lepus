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
import lepus.protocol.constants.*
import lepus.protocol.domains.*
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import ArbitraryDomains.given

object ExchangeDataGenerator {

  val declareGen: Gen[ExchangeClass.Declare] =
    for {
      arg0 <- Arbitrary.arbitrary[ExchangeName]
      arg1 <- Arbitrary.arbitrary[ShortString]
      arg2 <- Arbitrary.arbitrary[Boolean]
      arg3 <- Arbitrary.arbitrary[Boolean]
      arg4 <- Arbitrary.arbitrary[Boolean]
      arg5 <- Arbitrary.arbitrary[Boolean]
      arg6 <- Arbitrary.arbitrary[NoWait]
      arg7 <- Arbitrary.arbitrary[FieldTable]
    } yield ExchangeClass.Declare(
      arg0,
      arg1,
      arg2,
      arg3,
      arg4,
      arg5,
      arg6,
      arg7
    )

  given Arbitrary[ExchangeClass.Declare] = Arbitrary(declareGen)

  val declareOkGen: Gen[ExchangeClass.DeclareOk.type] =
    Gen.const(ExchangeClass.DeclareOk)

  given Arbitrary[ExchangeClass.DeclareOk.type] = Arbitrary(declareOkGen)

  val deleteGen: Gen[ExchangeClass.Delete] =
    for {
      arg0 <- Arbitrary.arbitrary[ExchangeName]
      arg1 <- Arbitrary.arbitrary[Boolean]
      arg2 <- Arbitrary.arbitrary[NoWait]
    } yield ExchangeClass.Delete(arg0, arg1, arg2)

  given Arbitrary[ExchangeClass.Delete] = Arbitrary(deleteGen)

  val deleteOkGen: Gen[ExchangeClass.DeleteOk.type] =
    Gen.const(ExchangeClass.DeleteOk)

  given Arbitrary[ExchangeClass.DeleteOk.type] = Arbitrary(deleteOkGen)

  val bindGen: Gen[ExchangeClass.Bind] =
    for {
      arg0 <- Arbitrary.arbitrary[ExchangeName]
      arg1 <- Arbitrary.arbitrary[ExchangeName]
      arg2 <- Arbitrary.arbitrary[ShortString]
      arg3 <- Arbitrary.arbitrary[NoWait]
      arg4 <- Arbitrary.arbitrary[FieldTable]
    } yield ExchangeClass.Bind(arg0, arg1, arg2, arg3, arg4)

  given Arbitrary[ExchangeClass.Bind] = Arbitrary(bindGen)

  val bindOkGen: Gen[ExchangeClass.BindOk.type] =
    Gen.const(ExchangeClass.BindOk)

  given Arbitrary[ExchangeClass.BindOk.type] = Arbitrary(bindOkGen)

  val unbindGen: Gen[ExchangeClass.Unbind] =
    for {
      arg0 <- Arbitrary.arbitrary[ExchangeName]
      arg1 <- Arbitrary.arbitrary[ExchangeName]
      arg2 <- Arbitrary.arbitrary[ShortString]
      arg3 <- Arbitrary.arbitrary[NoWait]
      arg4 <- Arbitrary.arbitrary[FieldTable]
    } yield ExchangeClass.Unbind(arg0, arg1, arg2, arg3, arg4)

  given Arbitrary[ExchangeClass.Unbind] = Arbitrary(unbindGen)

  val unbindOkGen: Gen[ExchangeClass.UnbindOk.type] =
    Gen.const(ExchangeClass.UnbindOk)

  given Arbitrary[ExchangeClass.UnbindOk.type] = Arbitrary(unbindOkGen)

  val classGen: Gen[ExchangeClass] = Gen.oneOf(
    declareGen,
    declareOkGen,
    deleteGen,
    deleteOkGen,
    bindGen,
    bindOkGen,
    unbindGen,
    unbindOkGen
  )

  given Arbitrary[ExchangeClass] = Arbitrary(classGen)

}
