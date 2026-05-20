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

class ConnectionSuite extends BrokerSuite {

  test("connects and exposes server capabilities") {
    connection.use { conn =>
      IO {
        // connection.blocked capability is advertised by RabbitMQ
        assert(conn.capabilities.connectionBlocked)
        assert(conn.capabilities.publisherConfirm)
      }
    }
  }

  test("opens a normal channel") {
    connection.use { conn =>
      conn.channel.use { ch =>
        ch.status.get.map { s =>
          assertEquals(s, Channel.Status.Active)
        }
      }
    }
  }

  test("opens a reliable channel") {
    connection.use { conn =>
      conn.reliableChannel.use { ch =>
        ch.status.get.map { s =>
          assertEquals(s, Channel.Status.Active)
        }
      }
    }
  }

  test("multiple channels can be open concurrently") {
    connection.use { conn =>
      (conn.channel, conn.channel, conn.channel).tupled.use { case (a, b, c) =>
        for {
          open <- conn.channels.get
          _ = assertEquals(open.size, 3)
          sa <- a.status.get
          sb <- b.status.get
          sc <- c.status.get
          _ = assertEquals(sa, Channel.Status.Active)
          _ = assertEquals(sb, Channel.Status.Active)
          _ = assertEquals(sc, Channel.Status.Active)
        } yield ()
      }
    }
  }
}
