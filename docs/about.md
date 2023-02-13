# About

```scala 
import cats.effect.IO
import cats.effect.IOApp
import com.comcast.ip4s.*
import lepus.client.Connection
import lepus.client.LepusClient
import lepus.client.Message
import lepus.protocol.domains.*
import scodec.bits.ByteVector

import scala.concurrent.duration.*
```

And then

```scala
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
```

and then

```scala
val exchange = ExchangeName("")

def run: IO[Unit] = connect.use((con, ch) =>
    for {
      _ <- IO.println(con.capabilities.toFieldTable)
      q <- ch.queue.declare(autoDelete = true)
      q <- IO.fromOption(q)(new Exception())
      print = ch.messaging.consume(q.queue).printlns
      publish = fs2.Stream
        .awakeEvery[IO](1.second)
        .map(_.toMillis)
        .evalTap(l => IO.println(s"publishing $l"))
        .map(l => Message(ByteVector.fromLong(l)))
        .evalMap(ch.messaging.publish(exchange, q.queue, _))
      _ <- IO.println(q)

      _ <- print.merge(publish).interruptAfter(10.seconds).compile.drain
    } yield ()
  )

```
