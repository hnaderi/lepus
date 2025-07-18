//> using scala 3.3
//> using dep "io.circe::circe-generic:0.14.5"
//> using dep "dev.hnaderi::named-codec-circe:0.1.0"
//> using dep "dev.hnaderi::lepus-std:0.5.4"
//> using dep "dev.hnaderi::lepus-circe:0.5.4"

package example

import cats.effect.*
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
