package lepus.client

import scodec.*
import scodec.bits.*
import scodec.codecs.*
import scodec.stream.StreamDecoder
import lepus.core.*
import cats.effect.*
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path

@main def app: Unit = {
  val firstCodec = uint8 :: uint8 :: uint16

  val result: Attempt[DecodeResult[(Int, Int, Int)]] =
    firstCodec.decode(hex"102a03ff".bits)

  println(result)

  val add3 = (_: Int) + (_: Int) + (_: Int)
  val sum: Attempt[DecodeResult[Int]] = result.map(_.map(add3.tupled))

  println(sum)
}

@main def typeTest: Unit = {
  val map: FieldTable =
    Map(
      ShortString("long") -> 12L,
      ShortString("int") -> 13,
      ShortString("short") -> ShortString("string")
    )

  println(ExchangeType("hello"))
  println(ExchangeType("topic"))

  println(map)
  map.values.foreach {
    case i: Int  => println(("integer", i))
    case l: Long => println(("long", l))
    case other   => println(("other", other))
  }
}

object DecodeTest extends IOApp {
  val decoder: StreamDecoder[ProtocolVersion | Frame] = StreamDecoder
    .once(lepus.client.codecs.protocol) ++ StreamDecoder.many(
    lepus.client.codecs.frame
  )

  // val input = Paths.get("client.bin")
  val app: Stream[IO, Unit] =
    Files[IO]
      .readAll(Path("client.bin"))
      .through(decoder.toPipeByte)
      .evalMap {
        case p: ProtocolVersion => IO.println(p)
        case f: Frame =>
          IO.println(s"""Type:\t${f.frameType}
Channel:\t${f.channel}
Size:\t${f.payload.size}
Payload:\t${String(f.payload)}
""")
      }

  def run(args: List[String]): IO[ExitCode] =
    app.compile.drain.as(ExitCode.Success)
}
