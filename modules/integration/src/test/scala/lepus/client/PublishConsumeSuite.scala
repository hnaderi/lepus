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
import cats.syntax.all.*
import lepus.protocol.domains.*

class PublishConsumeSuite extends BrokerSuite {

  test("publish and get a message synchronously") {
    connection.use { conn =>
      conn.channel.use { ch =>
        for {
          ok <- ch.queue.declare(autoDelete = true)
          q = ok.get.queue
          _ <- ch.messaging.publish(ExchangeName.default, q, "hello")
          result <- ch.messaging.get(q)
          _ = assert(result.isDefined)
        } yield ()
      }
    }
  }

  test("push consumer receives published message") {
    connection.use { conn =>
      conn.channel.use { ch =>
        for {
          ok <- ch.queue.declare(autoDelete = true)
          q = ok.get.queue
          _ <- ch.messaging.publish(ExchangeName.default, q, "world")
          msg <- ch.messaging.consume[String](q).take(1).compile.lastOrError
          _ = assertEquals(msg.message.payload, "world")
        } yield ()
      }
    }
  }

  test("message properties round-trip") {
    connection.use { conn =>
      conn.channel.use { ch =>
        for {
          ok <- ch.queue.declare(autoDelete = true)
          q = ok.get.queue
          env = Message("payload").withContentType(ShortString("text/plain"))
          _ <- ch.messaging.publish(ExchangeName.default, q, env)
          msg <- ch.messaging.consume[String](q).take(1).compile.lastOrError
          _ = assertEquals(msg.message.payload, "payload")
          _ = assertEquals(
            msg.message.properties.contentType,
            Some(ShortString("text/plain"))
          )
        } yield ()
      }
    }
  }

  test("declare exchange, bind queue, publish and consume via routing key") {
    connection.use { conn =>
      conn.channel.use { ch =>
        for {
          _ <- ch.exchange.declare(
            ExchangeName("test-direct"),
            ExchangeType.Direct,
            autoDelete = true
          )
          ok <- ch.queue.declare(autoDelete = true)
          q = ok.get.queue
          _ <- ch.queue.bind(q, ExchangeName("test-direct"), q)
          _ <- ch.messaging.publish(ExchangeName("test-direct"), q, "routed")
          msg <- ch.messaging.consume[String](q).take(1).compile.lastOrError
          _ = assertEquals(msg.message.payload, "routed")
        } yield ()
      }
    }
  }

  test("multiple messages preserve order") {
    val messages = List("one", "two", "three", "four", "five")
    connection.use { conn =>
      conn.channel.use { ch =>
        for {
          ok <- ch.queue.declare(autoDelete = true)
          q = ok.get.queue
          _ <- messages.traverse(m =>
            ch.messaging.publish(ExchangeName.default, q, m)
          )
          received <- ch.messaging
            .consume[String](q)
            .take(messages.size.toLong)
            .map(_.message.payload)
            .compile
            .toList
          _ = assertEquals(received, messages)
        } yield ()
      }
    }
  }
}
