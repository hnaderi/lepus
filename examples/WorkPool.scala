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

//> using scala 3.2
//> using dep "io.circe::circe-generic:0.14.5"
//> using dep "dev.hnaderi::named-codec-circe:0.1.0"
//> using dep "dev.hnaderi::lepus-std:0.5.3"
//> using dep "dev.hnaderi::lepus-circe:0.5.3"

package example

import cats.effect.*
import cats.syntax.all.*
import fs2.Stream
import lepus.client.*
import lepus.protocol.domains.*
import lepus.std.*

object WorkPool extends IOApp {

  private val protocol =
    WorkPoolDefinition(
      QueueName("jobs"),
      ChannelCodec.plain(MessageCodec.json[Task])
    )

  private val connection = Stream.resource(LepusClient[IO]())
  private val channel = connection.flatMap(con => Stream.resource(con.channel))

  val server =
    channel
      .evalMap(WorkPoolChannel.publisher(protocol, _))
      .flatMap(pool =>
        fs2.io
          .stdinUtf8(100)(using Async[IO])
          .map(Task(_))
          .evalMap(pool.publish)
      )

  def worker(name: String) = channel
    .evalMap(WorkPoolChannel.worker(protocol, _))
    .flatMap(pool =>
      pool.jobs
        .evalMap { job =>
          IO.println(s"worker $name: $job") >>
            pool.processed(job)
        }
    )

  override def run(args: List[String]): IO[ExitCode] =
    (args.map(_.toLowerCase) match {
      case "server" :: _         => server
      case "worker" :: name :: _ => worker(name)
      case _ => Stream.exec(IO.println(s"""Usage: workpool command
Commands:
  - server
  - worker <name>
"""))
    }).compile.drain.as(ExitCode.Success)
}

final case class Task(value: String) derives io.circe.Codec.AsObject
