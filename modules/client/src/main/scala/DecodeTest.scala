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
  val client: StreamDecoder[ProtocolVersion | Frame] =
    StreamDecoder.once(FrameCodec.protocol) ++
      StreamDecoder.many(FrameCodec.frame)

  val server: StreamDecoder[Frame] =
    StreamDecoder.many(FrameCodec.frame)

  val app: Stream[IO, Unit] =
    Files[IO]
      .readAll(Path("client.bin"))
      .through(client.toPipeByte)
      .evalMap(s => IO(println(s)))

  def run(args: List[String]): IO[ExitCode] =
    app.compile.drain.as(ExitCode.Success)
}
