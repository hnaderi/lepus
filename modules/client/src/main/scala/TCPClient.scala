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

package lepus.test

import cats.Applicative
import cats.Monad
import cats.MonadError
import cats.effect.MonadCancelThrow
import cats.effect.*
import cats.effect.std.Console
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.Chunk
import fs2.Pull
import fs2.Stream
import fs2.Stream.*
import fs2.interop.scodec.*
import fs2.io.file.FileHandle
import fs2.io.file.Files
import fs2.io.file.Flags
import fs2.io.file.Path
import fs2.io.file.WriteCursor
import fs2.io.net.Network
import fs2.io.net.Socket
import fs2.text
import lepus.client.DecodeTest
import lepus.client.Transport
import lepus.client.codecs.FrameCodec
import lepus.protocol.ProtocolVersion
import lepus.protocol.frame.Frame
import scodec.Attempt
import scodec.Encoder
import scodec.Err
import scodec.bits.ByteVector

import java.net.ConnectException
import java.nio.file.OpenOption
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import scala.concurrent.duration._

def connect[F[_]: Temporal: Network: Console](
    address: SocketAddress[Host]
): Stream[F, Socket[F]] =
  resource(Network[F].client(address))
    .handleErrorWith { case _: ConnectException =>
      eval(Console[F].error("Cannot connect...")) >>
        connect(address).delayBy(1.seconds)
    }

def proxy[F[_]: Files: Console: Network: Temporal](
    destination: SocketAddress[Host]
) =
  for {
    client <- Network[F].server(port = Some(port"5555")).head
    _ <- eval(Console[F].println("Client connected."))
    socket <- connect(destination)
    clientFile = Files[F].writeAll(Path("client.bin"), Flags.Write)
    serverFile = Files[F].writeAll(Path("server.bin"), Flags.Write)
    _ <- eval(Console[F].println("Proxy established."))
    send = client.reads.broadcastThrough(socket.writes, clientFile)
    recv = socket.reads.broadcastThrough(client.writes, serverFile)

    _ <- (send concurrently recv).onFinalize(
      Console[F].println("Proxy disconnected.")
    )
  } yield ()

extension [T](self: Attempt[T]) {
  def orRaise[F[_]](f: Err => Throwable)(using
      F: MonadError[F, Throwable]
  ): F[T] = self.fold(err => F.raiseError(f(err)), F.pure)
}

def inspector[F[_]: Files: Console: Network: Temporal](
    destination: SocketAddress[Host]
) =
  for {
    client <- Network[F].server(port = Some(port"5555")).head
    _ <- eval(Console[F].println("Client connected."))
    socket <- connect(destination)
    _ <- eval(Console[F].println("Proxy established."))
    protocol = Stream.chunk(
      Chunk.byteVector(ByteVector('A', 'M', 'Q', 'P', 0, 0, 9, 1))
    )
    // send = client.reads.broadcastThrough(
    //   socket.writes,
    //   _.through(Transport.decoderServer)
    //     .through(Transport.debug(true))
    // )
    // recv = socket.reads.broadcastThrough(
    //   client.writes,
    //   _.through(Transport.decoder)
    //     .through(Transport.debug(false))
    // )
    send = (protocol ++ client.reads
      .through(Transport.decoderServer)
      .through(Transport.debug(false))
      .through(Transport.encoder))
      .through(socket.writes)
    recv = socket.reads
      .through(Transport.decoder)
      .through(Transport.debug(true))
      .through(Transport.encoder)
      .through(client.writes)
    _ <- (send mergeHaltBoth recv).onFinalizeCase(e =>
      Console[F].println(s"Proxy disconnected. $e")
    )
  } yield ()

object Proxy extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    proxy[IO](SocketAddress(host"localhost", port"5672")).compile.drain
      .as(ExitCode.Success)
}

object Inspector extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    inspector[IO](SocketAddress(host"localhost", port"5672")).compile.drain
      .as(ExitCode.Success)
}
