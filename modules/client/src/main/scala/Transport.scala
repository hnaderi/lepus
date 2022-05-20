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

import cats.Functor
import cats.MonadError
import cats.effect.*
import cats.effect.std.Console
import com.comcast.ip4s.Host
import com.comcast.ip4s.SocketAddress
import fs2.Chunk
import fs2.Pipe
import fs2.Stream
import fs2.Stream.*
import fs2.interop.scodec.*
import fs2.io.net.Network
import fs2.io.net.Socket
import fs2.io.net.SocketOption
import lepus.protocol.*
import lepus.protocol.constants.ProtocolHeader
import lepus.wire.FrameCodec
import scodec.codecs.logFailuresToStdOut

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import scala.{Console => SCon}

type Transport[F[_]] = Pipe[F, Frame, Frame]

object Transport {
  private[lepus] def decoderServer[F[_]](using
      F: MonadError[F, Throwable]
  ): Pipe[F, Byte, Frame] =
    StreamDecoder
      .once(logFailuresToStdOut(FrameCodec.protocol))
      .flatMap(pv => StreamDecoder.many(logFailuresToStdOut(FrameCodec.frame)))
      .toPipeByte

  private[lepus] def decoder[F[_]](using
      F: MonadError[F, Throwable]
  ): Pipe[F, Byte, Frame] =
    StreamDecoder.many(logFailuresToStdOut(FrameCodec.frame)).toPipeByte

  private[lepus] def encoder[F[_]](using
      F: MonadError[F, Throwable]
  ): Pipe[F, Frame, Byte] =
    StreamEncoder.many(logFailuresToStdOut(FrameCodec.frame)).toPipeByte

  private val protocolHeader = chunk(
    Chunk.array(ProtocolHeader.getBytes(StandardCharsets.US_ASCII))
  )

  inline def debug[F[_]: Functor](
      send: Boolean
  )(using C: Console[F]): Pipe[F, Frame, Frame] =
    inline if send then
      _.evalTap(f => C.println(s"${SCon.CYAN}S> $f${SCon.RESET}"))
    else _.evalTap(f => C.println(s"${SCon.GREEN}C> $f${SCon.RESET}"))

  def build[F[_]: Concurrent](
      reads: Stream[F, Byte],
      writes: Pipe[F, Byte, Nothing]
  ): Transport[F] =
    toSend =>
      val recv = reads.through(decoderServer)
      val send = (protocolHeader ++ toSend.through(encoder)).through(writes)
      recv mergeHaltBoth send

  def fromSocket[F[_]: Concurrent](socket: Socket[F]): Transport[F] =
    build(socket.reads, socket.writes)

  def connect[F[_]: Concurrent: Network](
      address: SocketAddress[Host],
      options: List[SocketOption] = List.empty
  ): Transport[F] = in =>
    resource(Network[F].client(address, options))
      .flatMap(fromSocket(_).apply(in))
}
