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
import scodec.stream.StreamEncoder

object DecodeTest extends IOApp {
  private val fenc = logFailuresToStdOut(FrameCodec.frame, "FAILURE")

  val protocolEncoder: StreamEncoder[ProtocolVersion] =
    StreamEncoder.once(FrameCodec.protocol)
  val frameEncoder: StreamEncoder[Frame] = StreamEncoder.many(fenc)

  val client: StreamDecoder[ProtocolVersion | Frame] =
    StreamDecoder.once(FrameCodec.protocol) ++
      // StreamDecoder.many(FrameCodec.frame)
      StreamDecoder.many(fenc)

  val server: StreamDecoder[Frame] =
    StreamDecoder.many(fenc)

  val app: Stream[IO, Unit] =
    Files[IO]
      // .readAll(Path("server2.bin"))
      // .through(server.toPipeByte)
      .readAll(Path("client.bin"))
      .through(client.toPipeByte)
      .evalMap(s => IO(println(s)))

  def run(args: List[String]): IO[ExitCode] =
    app.compile.drain.as(ExitCode.Success)
}
