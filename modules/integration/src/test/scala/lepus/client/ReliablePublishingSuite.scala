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

import cats.effect.IO
import fs2.Stream
import lepus.protocol.domains.*

class ReliablePublishingSuite extends BrokerSuite {

  // Collects confirmations until `lastTag` is covered, handling both
  // individual acks and multiple=true batch acks from RabbitMQ.
  private def awaitConfirmations(
      lastTag: DeliveryTag,
      stream: Stream[IO, Confirmation]
  ): IO[List[Confirmation]] =
    stream.takeThrough(_.tag < lastTag).compile.toList

  test("published message is acked") {
    connection.use { conn =>
      conn.reliableChannel.use { ch =>
        for {
          ok <- ch.queue.declare(autoDelete = true)
          q = ok.get.queue
          t1 <- ch.messaging.publish(ExchangeName.default, q, "hello")
          confs <- awaitConfirmations(t1, ch.messaging.confirmations)
          _ = assert(confs.nonEmpty)
          _ = assert(confs.forall(_.kind == Acknowledgment.Ack))
        } yield ()
      }
    }
  }

  test("multiple published messages are all acked") {
    connection.use { conn =>
      conn.reliableChannel.use { ch =>
        for {
          ok <- ch.queue.declare(autoDelete = true)
          q = ok.get.queue
          _ <- ch.messaging.publish(ExchangeName.default, q, "msg1")
          _ <- ch.messaging.publish(ExchangeName.default, q, "msg2")
          t3 <- ch.messaging.publish(ExchangeName.default, q, "msg3")
          confs <- awaitConfirmations(t3, ch.messaging.confirmations)
          _ = assert(confs.nonEmpty)
          _ = assert(confs.forall(_.kind == Acknowledgment.Ack))
        } yield ()
      }
    }
  }

  test("delivery tags are monotonically increasing") {
    connection.use { conn =>
      conn.reliableChannel.use { ch =>
        for {
          ok <- ch.queue.declare(autoDelete = true)
          q = ok.get.queue
          t1 <- ch.messaging.publish(ExchangeName.default, q, "a")
          t2 <- ch.messaging.publish(ExchangeName.default, q, "b")
          t3 <- ch.messaging.publish(ExchangeName.default, q, "c")
          _ = assert(t1 < t2 && t2 < t3)
          _ <- awaitConfirmations(t3, ch.messaging.confirmations)
        } yield ()
      }
    }
  }
}
