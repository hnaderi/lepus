package lepus.client

import scodec.*
import scodec.bits.*
import scodec.codecs.*
import scodec.stream.StreamDecoder
import cats.effect.*
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import lepus.client.codecs.FrameCodec
import lepus.protocol.frame.*
import lepus.protocol.ProtocolVersion
import fs2.Pipe

object DecodeTest extends IOApp {
  val decoder: StreamDecoder[ProtocolVersion | Frame] =
    StreamDecoder.once(FrameCodec.protocol) ++
      StreamDecoder.many(FrameCodec.frame)

  // val input = Paths.get("client.bin")
  val app: Stream[IO, Unit] =
    Files[IO]
      .readAll(Path("client.bin"))
      .through(decoder.toPipeByte)
      .evalMap {
        case Frame(ch, FramePayload.Method(c, m, p)) =>
          IO.println(s"Channel: $ch Method class:$c id:$m") >>
            IO.println(s"Payload: ${String(p)}")
        case Frame(ch, FramePayload.Body(p)) =>
          IO.println(s"Channel: $ch\nContent: ${String(p)}")
        case other => IO.println(other)
      }

  def run(args: List[String]): IO[ExitCode] =
    app.compile.drain.as(ExitCode.Success)
}
