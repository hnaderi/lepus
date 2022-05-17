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

object ConnectionDataGenerator {

  val startGen: Gen[ConnectionClass.Start] =
    for {
      arg0 <- Arbitrary.arbitrary[Byte]
      arg1 <- Arbitrary.arbitrary[Byte]
      arg2 <- Arbitrary.arbitrary[PeerProperties]
      arg3 <- Arbitrary.arbitrary[LongString]
      arg4 <- Arbitrary.arbitrary[LongString]
    } yield ConnectionClass.Start(arg0, arg1, arg2, arg3, arg4)

  given Arbitrary[ConnectionClass.Start] = Arbitrary(startGen)

  val startOkGen: Gen[ConnectionClass.StartOk] =
    for {
      arg0 <- Arbitrary.arbitrary[PeerProperties]
      arg1 <- Arbitrary.arbitrary[ShortString]
      arg2 <- Arbitrary.arbitrary[LongString]
      arg3 <- Arbitrary.arbitrary[ShortString]
    } yield ConnectionClass.StartOk(arg0, arg1, arg2, arg3)

  given Arbitrary[ConnectionClass.StartOk] = Arbitrary(startOkGen)

  val secureGen: Gen[ConnectionClass.Secure] =
    for {
      arg0 <- Arbitrary.arbitrary[LongString]
    } yield ConnectionClass.Secure(arg0)

  given Arbitrary[ConnectionClass.Secure] = Arbitrary(secureGen)

  val secureOkGen: Gen[ConnectionClass.SecureOk] =
    for {
      arg0 <- Arbitrary.arbitrary[LongString]
    } yield ConnectionClass.SecureOk(arg0)

  given Arbitrary[ConnectionClass.SecureOk] = Arbitrary(secureOkGen)

  val tuneGen: Gen[ConnectionClass.Tune] =
    for {
      arg0 <- Arbitrary.arbitrary[Short]
      arg1 <- Arbitrary.arbitrary[Int]
      arg2 <- Arbitrary.arbitrary[Short]
    } yield ConnectionClass.Tune(arg0, arg1, arg2)

  given Arbitrary[ConnectionClass.Tune] = Arbitrary(tuneGen)

  val tuneOkGen: Gen[ConnectionClass.TuneOk] =
    for {
      arg0 <- Arbitrary.arbitrary[Short]
      arg1 <- Arbitrary.arbitrary[Int]
      arg2 <- Arbitrary.arbitrary[Short]
    } yield ConnectionClass.TuneOk(arg0, arg1, arg2)

  given Arbitrary[ConnectionClass.TuneOk] = Arbitrary(tuneOkGen)

  val openGen: Gen[ConnectionClass.Open] =
    for {
      arg0 <- Arbitrary.arbitrary[Path]
    } yield ConnectionClass.Open(arg0)

  given Arbitrary[ConnectionClass.Open] = Arbitrary(openGen)

  val openOkGen: Gen[ConnectionClass.OpenOk.type] =
    Gen.const(ConnectionClass.OpenOk)

  given Arbitrary[ConnectionClass.OpenOk.type] = Arbitrary(openOkGen)

  val closeGen: Gen[ConnectionClass.Close] =
    for {
      arg0 <- Arbitrary.arbitrary[ReplyCode]
      arg1 <- Arbitrary.arbitrary[ReplyText]
      arg2 <- Arbitrary.arbitrary[ClassId]
      arg3 <- Arbitrary.arbitrary[MethodId]
    } yield ConnectionClass.Close(arg0, arg1, arg2, arg3)

  given Arbitrary[ConnectionClass.Close] = Arbitrary(closeGen)

  val closeOkGen: Gen[ConnectionClass.CloseOk.type] =
    Gen.const(ConnectionClass.CloseOk)

  given Arbitrary[ConnectionClass.CloseOk.type] = Arbitrary(closeOkGen)

  val blockedGen: Gen[ConnectionClass.Blocked] =
    for {
      arg0 <- Arbitrary.arbitrary[ShortString]
    } yield ConnectionClass.Blocked(arg0)

  given Arbitrary[ConnectionClass.Blocked] = Arbitrary(blockedGen)

  val unblockedGen: Gen[ConnectionClass.Unblocked.type] =
    Gen.const(ConnectionClass.Unblocked)

  given Arbitrary[ConnectionClass.Unblocked.type] = Arbitrary(unblockedGen)

  val updateSecretGen: Gen[ConnectionClass.UpdateSecret] =
    for {
      arg0 <- Arbitrary.arbitrary[LongString]
      arg1 <- Arbitrary.arbitrary[ShortString]
    } yield ConnectionClass.UpdateSecret(arg0, arg1)

  given Arbitrary[ConnectionClass.UpdateSecret] = Arbitrary(updateSecretGen)

  val updateSecretOkGen: Gen[ConnectionClass.UpdateSecretOk.type] =
    Gen.const(ConnectionClass.UpdateSecretOk)

  given Arbitrary[ConnectionClass.UpdateSecretOk.type] = Arbitrary(
    updateSecretOkGen
  )

  val classGen: Gen[ConnectionClass] = Gen.oneOf(
    startGen,
    startOkGen,
    secureGen,
    secureOkGen,
    tuneGen,
    tuneOkGen,
    openGen,
    openOkGen,
    closeGen,
    closeOkGen,
    blockedGen,
    unblockedGen,
    updateSecretGen,
    updateSecretOkGen
  )

  given Arbitrary[ConnectionClass] = Arbitrary(classGen)

}
