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
import fs2.io.file.FileHandle
import fs2.io.file.Files
import fs2.io.file.Flags
import fs2.io.file.Path
import fs2.io.file.WriteCursor
import fs2.io.net.Network
import fs2.io.net.Socket
import fs2.text
import lepus.client.DecodeTest
import lepus.client.codecs.FrameCodec
import lepus.protocol.ProtocolVersion
import lepus.protocol.frame.Frame
import scodec.Attempt
import scodec.Encoder
import scodec.Err
import scodec.stream.CodecError

import java.net.ConnectException
import java.nio.file.OpenOption
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import scala.concurrent.duration._
import scodec.stream.StreamEncoder

def connect[F[_]: Temporal: Network: Console](
    address: SocketAddress[Host]
): Stream[F, Socket[F]] =
  resource(Network[F].client(address))
    .handleErrorWith { case _: ConnectException =>
      eval(Console[F].error("Cannot connect...")) >>
        connect(address).delayBy(1.seconds)
    }

def client[F[_]: Temporal: Console: Network: Files]: Stream[F, Unit] = for {
  socket <- connect(SocketAddress(host"localhost", port"5555"))
  _ <- Stream("Hello, world!")
    .interleave(Stream.constant("\n"))
    .through(text.utf8.encode)
    .through(socket.writes) ++
    socket.reads
      .through(text.utf8.decode)
      .through(text.lines)
      .head
      .foreach { response =>
        Console[F].println(s"Response: $response")
      }
} yield ()

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

import scala.{Console => SCon}
def inspector[F[_]: Files: Console: Network: Temporal](
    destination: SocketAddress[Host]
) =
  for {
    client <- Network[F].server(port = Some(port"5555")).head
    _ <- eval(Console[F].println("Client connected."))
    socket <- connect(destination)
    clientFile = DecodeTest.client.toPipeByte.andThen(
      _.evalTap(f => Console[F].println(s"${SCon.GREEN}<-- $f${SCon.RESET}"))
    )
    serverFile = DecodeTest.server.toPipeByte.andThen(
      _.evalTap(f => Console[F].println(s"${SCon.CYAN}--> $f${SCon.RESET}"))
    )
    _ <- eval(Console[F].println("Proxy established."))
    // send = client.reads.broadcastThrough(socket.writes, clientFile)
    // recv = socket.reads.broadcastThrough(client.writes, serverFile)

    send = client.reads.broadcastThrough(socket.writes, clientFile)
    // .through(clientFile)
    // .flatMap {
    //   case p: ProtocolVersion => DecodeTest.protocolEncoder.encode[F](emit(p))
    //   case f: Frame           => DecodeTest.frameEncoder.encode[F](emit(f))
    // }
    // .through(StreamEncoder.many(scodec.codecs.bits).toPipeByte)
    // .through(socket.writes)

    recv = socket.reads.broadcastThrough(client.writes, serverFile)
    // .through(serverFile)
    // .through(DecodeTest.frameEncoder.toPipeByte)
    // .through(client.writes)

    _ <- (send concurrently recv).onFinalizeCase(e =>
      Console[F].println(s"Proxy disconnected. $e")
    )
  } yield ()

def echoServer[F[_]: Concurrent: Network]: Stream[F, Unit] =
  Network[F]
    .server(port = Some(port"5555"))
    .map { client =>
      client.reads
        .through(text.utf8.decode)
        .through(text.lines)
        .interleave(Stream.constant("\n"))
        .through(text.utf8.encode)
        .through(client.writes)
        .handleErrorWith(_ => Stream.empty) // handle errors of client sockets
    }
    .parJoin(100)

object Client extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    client[IO].compile.drain.as(ExitCode.Success)
}
object Server extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    echoServer[IO].compile.drain.as(ExitCode.Success)
}
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
