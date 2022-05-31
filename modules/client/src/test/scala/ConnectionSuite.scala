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
import cats.effect.std.Queue
import lepus.protocol.*
import lepus.protocol.domains.*
import munit.CatsEffectSuite

abstract class ConnectionSuite extends CatsEffectSuite {
  test("") {
    val con: Connection[IO] = ???
    val app1 = con.channel.use(ch =>
      ch.messaging
        .consume(QueueName("abc"))
        .evalMap(msg => IO.println(msg.routingKey))
        .compile
        .drain
    )

    val app2 = con.reliableChannel.use(ch => ???)
    val app3 = con.transactionalChannel.use(ch =>
      ch.messaging.transaction.use(trx =>
        ch.messaging
          .publish(
            ExchangeName("hello"),
            ShortString("havij"),
            ???
          ) >> trx.commit
      )
    )

    val ss = ShortString.from("abs")
    val qn = QueueName("abs")
    val p = MessageCount(10)
  }

  private def method(m: Method) = Frame.Method(ChannelNumber(0), m)

  test("Sanity") {
    for {
      fs <- FakeServer()
      con <- Connection
        .from(fs.transport)
        .use(_ =>
          fs.send(
            method(
              ConnectionClass.Start(
                0,
                9,
                FieldTable.empty,
                LongString(""),
                locales = LongString("")
              )
            )
          ) >>
            fs.recv.assertEquals(
              method(
                ConnectionClass.StartOk(
                  FieldTable.empty,
                  mechanism = ShortString(""),
                  response = LongString(""),
                  locale = ShortString("")
                )
              )
            )
        )
    } yield ()
  }
}

final class FakeServer(sendQ: Queue[IO, Frame], recvQ: Queue[IO, Frame]) {
  def send(f: Frame): IO[Unit] = sendQ.offer(f)
  def recv: IO[Frame] = recvQ.take
  def transport: Transport[IO] = _.foreach(recvQ.offer).mergeHaltBoth(
    fs2.Stream.fromQueueUnterminated(sendQ)
  )
}
object FakeServer {
  def apply(): IO[FakeServer] = for {
    sQ <- Queue.unbounded[IO, Frame]
    rQ <- Queue.unbounded[IO, Frame]
  } yield new FakeServer(sQ, rQ)
}
