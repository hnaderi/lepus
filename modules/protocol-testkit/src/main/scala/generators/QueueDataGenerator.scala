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
import ArbitraryDomains.given

object QueueDataGenerator {
  val declareGen: Gen[QueueClass.Declare] =
    for {
      arg0 <- Arbitrary.arbitrary[QueueName]
      arg1 <- Arbitrary.arbitrary[Boolean]
      arg2 <- Arbitrary.arbitrary[Boolean]
      arg3 <- Arbitrary.arbitrary[Boolean]
      arg4 <- Arbitrary.arbitrary[Boolean]
      arg5 <- Arbitrary.arbitrary[NoWait]
      arg6 <- Arbitrary.arbitrary[FieldTable]
    } yield QueueClass.Declare(arg0, arg1, arg2, arg3, arg4, arg5, arg6)

  given Arbitrary[QueueClass.Declare] = Arbitrary(declareGen)

  val declareOkGen: Gen[QueueClass.DeclareOk] =
    for {
      arg0 <- Arbitrary.arbitrary[QueueName]
      arg1 <- Arbitrary.arbitrary[MessageCount]
      arg2 <- Arbitrary.arbitrary[Int]
    } yield QueueClass.DeclareOk(arg0, arg1, arg2)

  given Arbitrary[QueueClass.DeclareOk] = Arbitrary(declareOkGen)

  val bindGen: Gen[QueueClass.Bind] =
    for {
      arg0 <- Arbitrary.arbitrary[QueueName]
      arg1 <- Arbitrary.arbitrary[ExchangeName]
      arg2 <- Arbitrary.arbitrary[ShortString]
      arg3 <- Arbitrary.arbitrary[NoWait]
      arg4 <- Arbitrary.arbitrary[FieldTable]
    } yield QueueClass.Bind(arg0, arg1, arg2, arg3, arg4)

  given Arbitrary[QueueClass.Bind] = Arbitrary(bindGen)

  val bindOkGen: Gen[QueueClass.BindOk.type] =
    Gen.const(QueueClass.BindOk)

  given Arbitrary[QueueClass.BindOk.type] = Arbitrary(bindOkGen)

  val unbindGen: Gen[QueueClass.Unbind] =
    for {
      arg0 <- Arbitrary.arbitrary[QueueName]
      arg1 <- Arbitrary.arbitrary[ExchangeName]
      arg2 <- Arbitrary.arbitrary[ShortString]
      arg3 <- Arbitrary.arbitrary[FieldTable]
    } yield QueueClass.Unbind(arg0, arg1, arg2, arg3)

  given Arbitrary[QueueClass.Unbind] = Arbitrary(unbindGen)

  val unbindOkGen: Gen[QueueClass.UnbindOk.type] =
    Gen.const(QueueClass.UnbindOk)

  given Arbitrary[QueueClass.UnbindOk.type] = Arbitrary(unbindOkGen)

  val purgeGen: Gen[QueueClass.Purge] =
    for {
      arg0 <- Arbitrary.arbitrary[QueueName]
      arg1 <- Arbitrary.arbitrary[NoWait]
    } yield QueueClass.Purge(arg0, arg1)

  given Arbitrary[QueueClass.Purge] = Arbitrary(purgeGen)

  val purgeOkGen: Gen[QueueClass.PurgeOk] =
    for {
      arg0 <- Arbitrary.arbitrary[MessageCount]
    } yield QueueClass.PurgeOk(arg0)

  given Arbitrary[QueueClass.PurgeOk] = Arbitrary(purgeOkGen)

  val deleteGen: Gen[QueueClass.Delete] =
    for {
      arg0 <- Arbitrary.arbitrary[QueueName]
      arg1 <- Arbitrary.arbitrary[Boolean]
      arg2 <- Arbitrary.arbitrary[Boolean]
      arg3 <- Arbitrary.arbitrary[NoWait]
    } yield QueueClass.Delete(arg0, arg1, arg2, arg3)

  given Arbitrary[QueueClass.Delete] = Arbitrary(deleteGen)

  val deleteOkGen: Gen[QueueClass.DeleteOk] =
    for {
      arg0 <- Arbitrary.arbitrary[MessageCount]
    } yield QueueClass.DeleteOk(arg0)

  given Arbitrary[QueueClass.DeleteOk] = Arbitrary(deleteOkGen)

  val classGen: Gen[QueueClass] = Gen.oneOf(
    declareGen,
    declareOkGen,
    bindGen,
    bindOkGen,
    unbindGen,
    unbindOkGen,
    purgeGen,
    purgeOkGen,
    deleteGen,
    deleteOkGen
  )
  given Arbitrary[QueueClass] = Arbitrary(classGen)
}
