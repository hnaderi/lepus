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

package com.example

import cats.effect.IO
import cats.effect.IOApp
import com.comcast.ip4s.*
import lepus.client.Connection
import lepus.client.LepusClient
import lepus.protocol.domains.*

object Main extends IOApp.Simple {

  override def run: IO[Unit] = connect.use((con, ch) =>
    for {
      q <- ch.queue.declare(
        QueueName(""),
        passive = false,
        durable = false,
        exclusive = false,
        autoDelete = true,
        noWait = false,
        FieldTable.empty
      )
      _ <- IO.println(q)
    } yield ()
  )

  val connect = for {
    con <- LepusClient[IO](host"localhost", port"5672", "guest", "guest")
    _ <- con.status.discrete
      .foreach(s => IO.println(s"connection: $s"))
      .compile
      .drain
      .background
    ch <- con.channel
    _ <- ch.status.discrete
      .foreach(s => IO.println(s"channel: $s"))
      .compile
      .drain
      .background
  } yield (con, ch)
}
