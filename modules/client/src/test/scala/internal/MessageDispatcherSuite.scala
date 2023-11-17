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

package lepus.client
package internal

import cats.effect.IO
import lepus.codecs.BasicDataGenerator
import lepus.codecs.DomainGenerators
import lepus.codecs.FrameGenerators
import munit.CatsEffectSuite
import org.scalacheck.Arbitrary
import org.scalacheck.Gen

class MessageDispatcherSuite extends InternalTestSuite {
  private val consumers = DomainGenerators.consumerTag
  private val deliveries: Gen[DeliveredMessageRaw] = for {
    ctag <- consumers
    dtag <- DomainGenerators.deliveryTag
    rdlv <- Arbitrary.arbitrary[Boolean]
    ex <- DomainGenerators.exchangeName
    rkey <- DomainGenerators.shortString
    props <- DomainGenerators.properties
    data <- FrameGenerators.blob
    msg = MessageRaw(data, props)
  } yield DeliveredMessageRaw(ctag, dtag, rdlv, ex, rkey, msg)
  private val returns: Gen[ReturnedMessageRaw] = for {
    ex <- DomainGenerators.exchangeName
    rcode <- DomainGenerators.replyCode
    rtxt <- DomainGenerators.shortString
    rkey <- DomainGenerators.shortString
    props <- DomainGenerators.properties
    data <- FrameGenerators.blob
    msg = MessageRaw(data, props)
  } yield ReturnedMessageRaw(rcode, rtxt, ex, rkey, msg)
  private val confirmations: Gen[ConfirmationResponse] =
    Gen.oneOf(BasicDataGenerator.ackGen, BasicDataGenerator.nackGen)

  test("Must dispatch delivered messages") {
    forAllF(deliveries) { someMsg =>
      for {
        d <- MessageDispatcher[IO]()
        _ <- d.deliveryQ.use { (ctag, q) =>
          val msg = someMsg.copy(consumerTag = ctag)

          q.size.assertEquals(0) >>
            d.deliver(msg) >>
            q.size.assertEquals(1) >>
            q.take.assertEquals(Some(msg))
        }
      } yield ()
    }
  }

  test("Must dispatch consumer cancel") {
    for {
      d <- MessageDispatcher[IO]()
      _ <- d.deliveryQ.use { (ctag, q) =>
        q.size.assertEquals(0) >>
          d.cancel(ctag) >>
          q.size.assertEquals(1) >>
          q.take.assertEquals(None)
      }
    } yield ()
  }

  test("Must ignore messages after removing the queue") {
    // We are using async consumer cancel,
    // So we might receive messages after cancelling the consumer
    forAllF(deliveries) { someMsg =>
      for {
        d <- MessageDispatcher[IO]()
        out <- d.deliveryQ.use { (ctag, q) =>
          val msg = someMsg.copy(consumerTag = ctag)

          IO(msg, q)
        }
        (msg, q) = out
        _ <- q.size.assertEquals(0)
        _ <- d.deliver(msg)
        _ <- q.size.assertEquals(0)
      } yield ()
    }
  }

  test("Must dispatch returned messages") {
    forAllF(returns) { msg =>
      for {
        d <- MessageDispatcher[IO]()
        _ <- d.returnQ.size.assertEquals(0)
        _ <- d.`return`(msg)
        _ <- d.returnQ.size.assertEquals(1)
        _ <- d.returnQ.take.assertEquals(msg)
      } yield ()
    }
  }

  test("Must dispatch confirmation messages") {
    forAllF(confirmations) { msg =>
      for {
        d <- MessageDispatcher[IO]()
        _ <- d.confirmationQ.size.assertEquals(0)
        _ <- d.confirm(msg)
        _ <- d.confirmationQ.size.assertEquals(1)
        _ <- d.confirmationQ.take.assertEquals(msg)
      } yield ()
    }
  }
}
