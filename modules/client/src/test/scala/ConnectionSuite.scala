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
import munit.CatsEffectSuite

abstract class ConnectionSuite extends CatsEffectSuite {
  test("") {
    val con: Connection[IO] = ???
    val app1 = con.apiChannel.use(ch =>
      ch.queue.declare(???, ???, ???, ???, ???, ???, ???)
    )
    val app2 = con.channel.use(ch =>
      ch.messaging
        .consume(QueueName("abc"))
        .evalMap(msg => IO.println(msg.routingKey))
        .compile
        .drain
    )

    val app3 = con.reliableChannel.use(ch => ???)

    val ss = ShortString.from("abs")
    val qn = QueueName("abs")
    val p = MessageCount(10)
  }
}
