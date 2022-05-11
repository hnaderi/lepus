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
