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
import cats.implicits.*
import lepus.codecs.DomainGenerators
import lepus.codecs.FrameGenerators
import lepus.protocol.domains.ConsumerTag
import munit.CatsEffectSuite
import munit.ScalaCheckEffectSuite
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.effect.PropF.forAllF

class MessageDispatcherSuite extends InternalTestSuite {
  private val consumers = DomainGenerators.consumerTag
  private val deliveries: Gen[DeliveredMessage] = for {
    ctag <- consumers
    dtag <- DomainGenerators.deliveryTag
    rdlv <- Arbitrary.arbitrary[Boolean]
    ex <- DomainGenerators.exchangeName
    rkey <- DomainGenerators.shortString
    props <- DomainGenerators.properties
    data <- FrameGenerators.blob
    msg = Message(data, props)
  } yield DeliveredMessage(ctag, dtag, rdlv, ex, rkey, msg)
  private val returns: Gen[ReturnedMessage] = for {
    ex <- DomainGenerators.exchangeName
    rcode <- DomainGenerators.replyCode
    rtxt <- DomainGenerators.shortString
    rkey <- DomainGenerators.shortString
    props <- DomainGenerators.properties
    data <- FrameGenerators.blob
    msg = Message(data, props)
  } yield ReturnedMessage(rcode, rtxt, ex, rkey, msg)

  private def assertDelivered(d: MessageDispatcher[IO], msg: DeliveredMessage) =
    d
      .deliveryQ(msg.consumerTag)
      .use(q =>
        q.size.assertEquals(0) >>
          d.deliver(msg) >>
          q.size.assertEquals(1) >>
          q.take.assertEquals(msg)
      )

  test("Must dispatch delivered messages") {
    forAllF(deliveries) { msg =>
      for {
        d <- MessageDispatcher[IO]
        _ <- assertDelivered(d, msg)
      } yield ()
    }
  }

  test("Must allow reusing consumer tags") {
    forAllF(deliveries) { msg =>
      for {
        d <- MessageDispatcher[IO]
        _ <- assertDelivered(d, msg)
        _ <- assertDelivered(d, msg)
      } yield ()
    }
  }

  test("Must throw error on duplicated consumer tag retrieval") {
    val ctag = ConsumerTag.empty

    for {
      d <- MessageDispatcher[IO]
      _ <- d
        .deliveryQ(ctag)
        .use(_ =>
          d.deliveryQ(ctag)
            .use_
            .attempt
            .assertEquals(MessageDispatcher.AlreadyExists(ctag).asLeft)
        )
    } yield ()
  }

  test("Must dispatch returned messages") {
    forAllF(returns) { msg =>
      for {
        d <- MessageDispatcher[IO]
        _ <- d.returnQ.size.assertEquals(0)
        _ <- d.`return`(msg)
        _ <- d.returnQ.size.assertEquals(1)
        _ <- d.returnQ.take.assertEquals(msg)
      } yield ()
    }
  }
}
