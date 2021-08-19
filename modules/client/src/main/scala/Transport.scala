package lepus.client

import cats.effect.*
import fs2.Stream
import fs2.Stream.*
import lepus.client.codecs.MyCodecs
import lepus.protocol.frame.*
import lepus.protocol.ProtocolVersion
import fs2.Pipe
import scodec.stream.StreamDecoder
import scodec.stream.StreamEncoder
import cats.MonadError
import fs2.io.net.Network
import fs2.io.net.Socket
import com.comcast.ip4s.SocketAddress
import com.comcast.ip4s.Host
import fs2.io.net.SocketOption

type Transport[F[_]] = Pipe[F, Frame, Frame]
object Transport {
  def decoder[F[_]](using F: MonadError[F, Throwable]) =
    StreamDecoder.many(MyCodecs.frame).toPipeByte
  def encoder[F[_]](using F: MonadError[F, Throwable]) =
    StreamEncoder.many(MyCodecs.frame).toPipeByte

  def build[F[_]: Concurrent](
      reads: Stream[F, Byte],
      writes: Pipe[F, Byte, Nothing]
  ): Transport[F] =
    toSend =>
      val recv = reads.through(decoder)
      val send = toSend.through(encoder).through(writes)
      recv concurrently send

  def fromSocket[F[_]: Concurrent](socket: Socket[F]): Transport[F] =
    build(socket.reads, socket.writes)

  def connect[F[_]: Concurrent: Network](
      address: SocketAddress[Host],
      options: List[SocketOption] = List.empty
  ): Transport[F] = in =>
    resource(Network[F].client(address, options))
      .flatMap(fromSocket(_).apply(in))
}
