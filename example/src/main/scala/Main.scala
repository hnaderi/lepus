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

import cats.effect.IO
import cats.effect.IOApp
import com.comcast.ip4s.*
import lepus.client.*
import lepus.protocol.domains.*
import scodec.bits.ByteVector

import scala.concurrent.duration.*

object Main extends IOApp.Simple {

  private val exchange = ExchangeName.default

  def app(con: Connection[IO]) = con.channel.use(ch =>
    for {
      _ <- IO.println(con.capabilities.toFieldTable)
      _ <- ch.exchange.declare(ExchangeName("events"), ExchangeType.Topic)
      q <- ch.queue.declare(autoDelete = true)
      q <- IO.fromOption(q)(new Exception())
      print = ch.messaging
        .consume[String](q.queue, mode = ConsumeMode.NackOnError)
        .printlns
      publish = fs2.Stream
        .awakeEvery[IO](1.second)
        .map(_.toMillis)
        .evalTap(l => IO.println(s"publishing $l"))
        .map(l => Message(l.toString()))
        .evalMap(ch.messaging.publish(exchange, q.queue, _))
      _ <- IO.println(q)

      _ <- print.merge(publish).interruptAfter(10.seconds).compile.drain
    } yield ()
  )

  private val connect =
    // connect to default port
    LepusClient[IO](debug = true)
    // or connect to the TLS port
    // for more advanced ssl see SSLExample.scala under .jvm directory
    //
    // con <- LepusClient[IO](debug = true,port=port"5671", ssl = SSL.Trusted)

  override def run: IO[Unit] = connect.use(app)

}
