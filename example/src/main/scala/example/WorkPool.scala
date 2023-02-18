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

package example

import cats.effect.*
import cats.syntax.all.*
import lepus.client.*
import lepus.protocol.domains.*
import lepus.std.*

import scala.concurrent.duration.*

object WorkPool extends IOApp.Simple {

  private val protocol =
    WorkPoolDefinition(
      QueueName("jobs"),
      ChannelCodec.plain(MessageCodec.json[Task])
    )

  def server(con: Connection[IO]) = con.channel
    .evalMap(WorkPoolChannel.publisher(protocol, _))
    .use(pool => List.range(0, 100).map(Task(_)).traverse(pool.publish))

  def worker(con: Connection[IO])(number: Int) = con.channel
    .evalMap(WorkPoolChannel.worker(protocol, _))
    .use(pool =>
      pool.jobs
        .evalMap { job =>
          IO.println(s"worker $number: $job") >>
            pool.processed(job)
        }
        // This is needed in this example, in order to terminate application
        .interruptAfter(5.seconds)
        .compile
        .drain
    )

  override def run: IO[Unit] = LepusClient[IO]().use { con =>
    IO.both(
      server(con),
      List.range(0, 3).parTraverse(worker(con))
    ).void
  }
}

final case class Task(value: Int) derives io.circe.Codec.AsObject
