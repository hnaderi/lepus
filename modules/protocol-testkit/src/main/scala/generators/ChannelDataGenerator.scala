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

object ChannelDataGenerator {
  val openGen: Gen[ChannelClass.Open.type] =
    Gen.const(ChannelClass.Open)

  given Arbitrary[ChannelClass.Open.type] = Arbitrary(openGen)

  val openOkGen: Gen[ChannelClass.OpenOk.type] =
    Gen.const(ChannelClass.OpenOk)

  given Arbitrary[ChannelClass.OpenOk.type] = Arbitrary(openOkGen)

  val flowGen: Gen[ChannelClass.Flow] =
    for {
      arg0 <- Arbitrary.arbitrary[Boolean]
    } yield ChannelClass.Flow(arg0)

  given Arbitrary[ChannelClass.Flow] = Arbitrary(flowGen)

  val flowOkGen: Gen[ChannelClass.FlowOk] =
    for {
      arg0 <- Arbitrary.arbitrary[Boolean]
    } yield ChannelClass.FlowOk(arg0)

  given Arbitrary[ChannelClass.FlowOk] = Arbitrary(flowOkGen)

  val closeGen: Gen[ChannelClass.Close] =
    for {
      arg0 <- Arbitrary.arbitrary[ReplyCode]
      arg1 <- Arbitrary.arbitrary[ReplyText]
      arg2 <- Arbitrary.arbitrary[ClassId]
      arg3 <- Arbitrary.arbitrary[MethodId]
    } yield ChannelClass.Close(arg0, arg1, arg2, arg3)

  given Arbitrary[ChannelClass.Close] = Arbitrary(closeGen)

  val closeOkGen: Gen[ChannelClass.CloseOk.type] =
    Gen.const(ChannelClass.CloseOk)

  given Arbitrary[ChannelClass.CloseOk.type] = Arbitrary(closeOkGen)

  val classGen: Gen[ChannelClass] =
    Gen.oneOf(openGen, openOkGen, flowGen, flowOkGen, closeGen, closeOkGen)
  given Arbitrary[ChannelClass] = Arbitrary(classGen)
}
