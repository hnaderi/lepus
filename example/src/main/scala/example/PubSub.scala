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
//> using dep "dev.hnaderi::lepus-std:0.3.0"
//> using dep "dev.hnaderi::lepus-circe:0.3.0"

package example

import cats.effect.*
import cats.syntax.all.*
import dev.hnaderi.namedcodec.*
import fs2.Stream
import fs2.Stream.*
import io.circe.generic.auto.*
import lepus.circe.given
import lepus.client.*
import lepus.protocol.domains.*
import lepus.std.*

import scala.concurrent.duration.*

object PubSub extends IOApp.Simple {

  private val protocol =
    TopicDefinition(
      ExchangeName("events"),
      ChannelCodec.default(CirceAdapter.of[Event]),
      TopicNameEncoder.of[Event]
    )

  def publisher(con: Connection[IO]) = for {
    ch <- resource(con.channel)
    bus <- eval(EventChannel.publisher(protocol, ch))
    (toPublish, idx) <- Stream(
      Event.Created("b"),
      Event.Updated("a", 10),
      Event.Updated("b", 100),
      Event.Created("c")
    ).zipWithIndex
    _ <- eval(bus.publish(ShortString.from(idx), toPublish))
  } yield ()

  def consumer1(con: Connection[IO]) = for {
    ch <- resource(con.channel)
    bus <- eval(EventChannel.consumer(protocol)(ch))
    evt <- bus.events
    _ <- eval(IO.println(s"consumer 1: $evt"))
  } yield ()

  def consumer2(con: Connection[IO]) = for {
    ch <- resource(con.channel)
    bus <- eval(
      EventChannel.consumer(protocol, ch, TopicSelector("Created"))
    )
    evt <- bus.events
    _ <- eval(IO.println(s"consumer 2: $evt"))
  } yield ()

  override def run: IO[Unit] = LepusClient[IO]().use { con =>
    Stream(publisher(con), consumer1(con), consumer2(con)).parJoinUnbounded
      // This is needed in this example, in order to terminate application
      .interruptAfter(5.seconds)
      .compile
      .drain
  }
}

enum Event {
  case Created(id: String)
  case Updated(id: String, value: Int)
}
