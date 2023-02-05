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

import cats.effect.IOApp
import cats.effect.IO
import lepus.client.Connection
import lepus.client.LepusClient
import lepus.protocol.domains.*
import com.comcast.ip4s.*

object Main extends IOApp.Simple {

  override def run: IO[Unit] =
    LepusClient[IO](host"localhost", port"5672", "guest", "guest")
      .flatMap(_.channel)
      .use(ch =>
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
}
