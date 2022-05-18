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

object BasicDataGenerator {
  val qosGen: Gen[BasicClass.Qos] =
    for {
      arg0 <- Arbitrary.arbitrary[Int]
      arg1 <- Arbitrary.arbitrary[Short]
      arg2 <- Arbitrary.arbitrary[Boolean]
    } yield BasicClass.Qos(arg0, arg1, arg2)

  given Arbitrary[BasicClass.Qos] = Arbitrary(qosGen)

  val qosOkGen: Gen[BasicClass.QosOk.type] =
    Gen.const(BasicClass.QosOk)

  given Arbitrary[BasicClass.QosOk.type] = Arbitrary(qosOkGen)

  val consumeGen: Gen[BasicClass.Consume] =
    for {
      arg0 <- Arbitrary.arbitrary[QueueName]
      arg1 <- Arbitrary.arbitrary[ConsumerTag]
      arg2 <- Arbitrary.arbitrary[NoLocal]
      arg3 <- Arbitrary.arbitrary[NoAck]
      arg4 <- Arbitrary.arbitrary[Boolean]
      arg5 <- Arbitrary.arbitrary[NoWait]
      arg6 <- Arbitrary.arbitrary[FieldTable]
    } yield BasicClass.Consume(arg0, arg1, arg2, arg3, arg4, arg5, arg6)

  given Arbitrary[BasicClass.Consume] = Arbitrary(consumeGen)

  val consumeOkGen: Gen[BasicClass.ConsumeOk] =
    for {
      arg0 <- Arbitrary.arbitrary[ConsumerTag]
    } yield BasicClass.ConsumeOk(arg0)

  given Arbitrary[BasicClass.ConsumeOk] = Arbitrary(consumeOkGen)

  val cancelGen: Gen[BasicClass.Cancel] =
    for {
      arg0 <- Arbitrary.arbitrary[ConsumerTag]
      arg1 <- Arbitrary.arbitrary[NoWait]
    } yield BasicClass.Cancel(arg0, arg1)

  given Arbitrary[BasicClass.Cancel] = Arbitrary(cancelGen)

  val cancelOkGen: Gen[BasicClass.CancelOk] =
    for {
      arg0 <- Arbitrary.arbitrary[ConsumerTag]
    } yield BasicClass.CancelOk(arg0)

  given Arbitrary[BasicClass.CancelOk] = Arbitrary(cancelOkGen)

  val publishGen: Gen[BasicClass.Publish] =
    for {
      arg0 <- Arbitrary.arbitrary[ExchangeName]
      arg1 <- Arbitrary.arbitrary[ShortString]
      arg2 <- Arbitrary.arbitrary[Boolean]
      arg3 <- Arbitrary.arbitrary[Boolean]
    } yield BasicClass.Publish(arg0, arg1, arg2, arg3)

  given Arbitrary[BasicClass.Publish] = Arbitrary(publishGen)

  val returnGen: Gen[BasicClass.Return] =
    for {
      arg0 <- Arbitrary.arbitrary[ReplyCode]
      arg1 <- Arbitrary.arbitrary[ReplyText]
      arg2 <- Arbitrary.arbitrary[ExchangeName]
      arg3 <- Arbitrary.arbitrary[ShortString]
    } yield BasicClass.Return(arg0, arg1, arg2, arg3)

  given Arbitrary[BasicClass.Return] = Arbitrary(returnGen)

  val deliverGen: Gen[BasicClass.Deliver] =
    for {
      arg0 <- Arbitrary.arbitrary[ConsumerTag]
      arg1 <- Arbitrary.arbitrary[DeliveryTag]
      arg2 <- Arbitrary.arbitrary[Redelivered]
      arg3 <- Arbitrary.arbitrary[ExchangeName]
      arg4 <- Arbitrary.arbitrary[ShortString]
    } yield BasicClass.Deliver(arg0, arg1, arg2, arg3, arg4)

  given Arbitrary[BasicClass.Deliver] = Arbitrary(deliverGen)

  val getGen: Gen[BasicClass.Get] =
    for {
      arg0 <- Arbitrary.arbitrary[QueueName]
      arg1 <- Arbitrary.arbitrary[NoAck]
    } yield BasicClass.Get(arg0, arg1)

  given Arbitrary[BasicClass.Get] = Arbitrary(getGen)

  val getOkGen: Gen[BasicClass.GetOk] =
    for {
      arg0 <- Arbitrary.arbitrary[DeliveryTag]
      arg1 <- Arbitrary.arbitrary[Redelivered]
      arg2 <- Arbitrary.arbitrary[ExchangeName]
      arg3 <- Arbitrary.arbitrary[ShortString]
      arg4 <- Arbitrary.arbitrary[MessageCount]
    } yield BasicClass.GetOk(arg0, arg1, arg2, arg3, arg4)

  given Arbitrary[BasicClass.GetOk] = Arbitrary(getOkGen)

  val getEmptyGen: Gen[BasicClass.GetEmpty.type] =
    Gen.const(BasicClass.GetEmpty)

  given Arbitrary[BasicClass.GetEmpty.type] = Arbitrary(getEmptyGen)

  val ackGen: Gen[BasicClass.Ack] =
    for {
      arg0 <- Arbitrary.arbitrary[DeliveryTag]
      arg1 <- Arbitrary.arbitrary[Boolean]
    } yield BasicClass.Ack(arg0, arg1)

  given Arbitrary[BasicClass.Ack] = Arbitrary(ackGen)

  val rejectGen: Gen[BasicClass.Reject] =
    for {
      arg0 <- Arbitrary.arbitrary[DeliveryTag]
      arg1 <- Arbitrary.arbitrary[Boolean]
    } yield BasicClass.Reject(arg0, arg1)

  given Arbitrary[BasicClass.Reject] = Arbitrary(rejectGen)

  val recoverAsyncGen: Gen[BasicClass.RecoverAsync] =
    for {
      arg0 <- Arbitrary.arbitrary[Boolean]
    } yield BasicClass.RecoverAsync(arg0)

  given Arbitrary[BasicClass.RecoverAsync] = Arbitrary(recoverAsyncGen)

  val recoverGen: Gen[BasicClass.Recover] =
    for {
      arg0 <- Arbitrary.arbitrary[Boolean]
    } yield BasicClass.Recover(arg0)

  given Arbitrary[BasicClass.Recover] = Arbitrary(recoverGen)

  val recoverOkGen: Gen[BasicClass.RecoverOk.type] =
    Gen.const(BasicClass.RecoverOk)

  given Arbitrary[BasicClass.RecoverOk.type] = Arbitrary(recoverOkGen)

  val nackGen: Gen[BasicClass.Nack] =
    for {
      arg0 <- Arbitrary.arbitrary[DeliveryTag]
      arg1 <- Arbitrary.arbitrary[Boolean]
      arg2 <- Arbitrary.arbitrary[Boolean]
    } yield BasicClass.Nack(arg0, arg1, arg2)

  given Arbitrary[BasicClass.Nack] = Arbitrary(nackGen)

  val classGen: Gen[BasicClass] = Gen.oneOf(
    qosGen,
    qosOkGen,
    consumeGen,
    consumeOkGen,
    cancelGen,
    cancelOkGen,
    publishGen,
    returnGen,
    deliverGen,
    getGen,
    getOkGen,
    getEmptyGen,
    ackGen,
    rejectGen,
    recoverAsyncGen,
    recoverGen,
    recoverOkGen,
    nackGen
  )
  given Arbitrary[BasicClass] = Arbitrary(classGen)
}
