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

import com.rabbitmq.client.impl.AMQImpl
import lepus.protocol.*
import lepus.protocol.domains.*
import lepus.wire.MethodCodec
import munit.FunSuite
import munit.Location
import munit.ScalaCheckSuite
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Prop
import org.scalacheck.Prop._
import scodec.*

import DomainGenerators.*
import DataTest.*

class DataTest extends ScalaCheckSuite {

  // property("Field tables official") {
  //   forAll(fieldTable) { table =>
  //     val res = DomainCodecs.fieldTable.encode(table).map(_.toHex)
  //     val official = writeMap(table).toHex

  //     res.toEither.map(assertEquals(_, official)).getOrElse(fail(""))
  //   }
  // }

  property("Deletes") {
    forAll(deletes) { d =>
      val df = AMQImpl.Exchange.Delete(0, d.exchange, d.ifUnused, d.noWait)
      val officialOut = writeMethod(df)
      val lepusRead = MethodCodec.all.decode(officialOut)
      val lepusOut = MethodCodec.all.encode(d).getOrElse(???)
      assertEquals(bitSplit(officialOut.toBin), bitSplit(lepusOut.toBin))
      assertEquals(Attempt.successful[Method](d), lepusRead.map(_.value))
    }
  }

  property("Queue declare") {
    forAll(qDeclare) { d =>
      val df = AMQImpl.Queue.Declare(
        0,
        d.queue,
        d._2,
        d._3,
        d._4,
        d._5,
        d._6,
        new java.util.HashMap()
      )
      val officialOut = writeMethod(df)
      val lepusRead = MethodCodec.all.decode(officialOut)
      val lepusOut = MethodCodec.all.encode(d).getOrElse(???)
      assertEquals(bitSplit(officialOut.toBin), bitSplit(lepusOut.toBin))
      assertEquals(Attempt.successful[Method](d), lepusRead.map(_.value))
    }
  }
}

object DataTest {
  val deletes: Gen[ExchangeClass.Delete] = for {
    ex <- exchangeName
    a <- Arbitrary.arbitrary[Boolean]
    b <- Arbitrary.arbitrary[Boolean]
  } yield ExchangeClass.Delete(ex, a, b)

  val qDeclare: Gen[QueueClass.Declare] = for {
    qName <- queueName
    a <- Arbitrary.arbitrary[Boolean]
    b <- Arbitrary.arbitrary[Boolean]
    c <- Arbitrary.arbitrary[Boolean]
    d <- Arbitrary.arbitrary[Boolean]
    e <- Arbitrary.arbitrary[Boolean]
  } yield QueueClass.Declare(qName, a, b, c, d, e, FieldTable.empty)
}
