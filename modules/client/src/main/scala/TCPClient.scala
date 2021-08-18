import fs2.{Chunk, Stream, text}
import fs2.io.net.Network
import cats.effect.MonadCancelThrow
import cats.effect.std.Console
import cats.syntax.all._
import com.comcast.ip4s._
import cats.effect.*
import scala.concurrent.duration._
import fs2.io.net.Socket
import java.net.ConnectException
import fs2.Stream.*
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.file.FileHandle
import java.nio.file.Paths
import java.nio.file.OpenOption
import java.nio.file.StandardOpenOption
import fs2.io.file.WriteCursor
import fs2.io.file.Flags

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
