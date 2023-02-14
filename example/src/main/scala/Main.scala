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
import lepus.client.MessageRaw
import lepus.protocol.domains.*
import scodec.bits.ByteVector

import scala.concurrent.duration.*

object Main extends IOApp.Simple {

  private val exchange = ExchangeName("")

  override def run: IO[Unit] = connect.use((con, ch) =>
    for {
      _ <- IO.println(con.capabilities.toFieldTable)
      q <- ch.queue.declare(autoDelete = true)
      q <- IO.fromOption(q)(new Exception())
      print = ch.messaging.consume(q.queue).printlns
      publish = fs2.Stream
        .awakeEvery[IO](1.second)
        .map(_.toMillis)
        .evalTap(l => IO.println(s"publishing $l"))
        .map(l => MessageRaw(ByteVector.fromLong(l)))
        .evalMap(ch.messaging.publishRaw(exchange, q.queue, _))
      _ <- IO.println(q)

      _ <- print.merge(publish).interruptAfter(10.seconds).compile.drain
    } yield ()
  )

  val connect = for {
    con <- LepusClient[IO](debug = true)
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
