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
import lepus.protocol.domains.*

class TransactionalSuite extends BrokerSuite {

  test("committed transaction delivers messages") {
    connection.use { conn =>
      conn.transactionalChannel.use { ch =>
        for {
          ok <- ch.queue.declare(autoDelete = true)
          q = ok.get.queue
          _ <- ch.messaging.transaction.use { _ =>
            ch.messaging.publish(ExchangeName.default, q, "committed")
          }
          result <- ch.messaging.get(q)
          _ = assert(result.isDefined)
        } yield ()
      }
    }
  }

  test("rolled-back transaction discards messages") {
    connection.use { conn =>
      conn.transactionalChannel.use { ch =>
        for {
          ok <- ch.queue.declare(autoDelete = true)
          q = ok.get.queue
          _ <- ch.messaging.transaction
            .use { _ =>
              ch.messaging.publish(ExchangeName.default, q, "discarded") >>
                IO.raiseError(new RuntimeException("deliberate rollback"))
            }
            .handleError(_ => ())
          result <- ch.messaging.get(q)
          _ = assert(result.isEmpty)
        } yield ()
      }
    }
  }
}
