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

import cats.effect.Concurrent
import cats.effect.Resource
import cats.effect.implicits.*
import cats.effect.std.Queue
import cats.implicits.*
import fs2.Pipe
import fs2.Stream
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import lepus.client.Connection.Status
import lepus.protocol.*
import lepus.protocol.domains.ChannelNumber

import internal.*
import cats.effect.kernel.Ref

trait Connection[F[_]] {
  def apiChannel: Resource[F, APIChannel[F]]
  def channel: Resource[F, MessagingChannel[F]]
  def reliableChannel: Resource[F, ReliableMessagingChannel[F]]

  def status: Signal[F, Connection.Status]
}

object Connection {
  def apply[F[_]: Concurrent](
      transport: Transport[F],
      bufferSize: Int = 100
  ): Resource[F, Connection[F]] = for {
    _status <- Resource.eval(SignallingRef[F].of(Status.New))
    sendQ <- Resource.eval(Queue.bounded[F, Frame](bufferSize))
    con = ConnectionImpl(sendQ, _status, bufferSize, ???)
    _ <- con.run(transport).compile.drain.background
  } yield con

  enum Status {
    case New, Connecting, Connected, Closed
  }
}

private final class ConnectionImpl[F[_]: Concurrent](
    sendQ: Queue[F, Frame],
    _status: SignallingRef[F, Status],
    bufferSize: Int,
    channels: Ref[F, Map[ChannelNumber, ChannelReceiver[F]]]
) extends Connection[F] {
  def apiChannel: Resource[F, APIChannel[F]] = ???
  def channel: Resource[F, MessagingChannel[F]] = ???
  def reliableChannel: Resource[F, ReliableMessagingChannel[F]] = ???
  def status: Signal[F, Connection.Status] = ???

  private def getChannel(ch: ChannelNumber): F[ChannelReceiver[F]] =
    channels.get.flatMap(_.get(ch).fold(???)(_.pure))

  private def handleAsync(ch: ChannelReceiver[F]): Metadata.Async => F[Unit] = {
    case m: BasicClass.Deliver          => ch.asyncNotify(m).flatMap(???)
    case ConnectionClass.Blocked(_)     => ???
    case ConnectionClass.Unblocked      => ???
    case ChannelClass.FlowOk(_)         => ???
    case m: BasicClass.Return           => ch.asyncNotify(m).flatMap(???)
    case BasicClass.Ack(_, _)           => ???
    case BasicClass.Reject(_, _)        => ???
    case BasicClass.RecoverAsync(_)     => ???
    case BasicClass.Recover(_)          => ???
    case BasicClass.Nack(_, _, _)       => ???
    case BasicClass.Publish(_, _, _, _) => ??? // won't happen
  }

  private def handleMethod(ch: ChannelReceiver[F]): Method => F[Unit] = {
    case m: (Metadata.ServerMethod & Metadata.Response) => ch.recv(m).as(???)
    case m: Metadata.Async                              => handleAsync(ch)(m)
    case m: ConnectionClass.Start                       => ???
    case m: ConnectionClass.Secure                      => ???
    case m: ConnectionClass.Tune                        => ???
    case m: ConnectionClass.Close                       => ???
    case ConnectionClass.CloseOk                        => ???
    case m: ConnectionClass.UpdateSecret                => ???
    case m: ChannelClass.Flow                           => ???
    case m: ChannelClass.Close                          => ???
    case ChannelClass.CloseOk                           => ???
    case m: BasicClass.Cancel                           => ???
    case m: BasicClass.CancelOk                         => ???
    case BasicClass.RecoverOk                           => ???
    case m: Metadata.ClientMethod                       => ???
  }

  private[client] def run(transport: Transport[F]): Stream[F, Nothing] =
    Stream
      .fromQueueUnterminated(sendQ, bufferSize)
      .through(transport)
      .evalMap {
        case f @ Frame.Body(ch, _) =>
          getChannel(ch).flatMap(_.recv(f)).flatMap(???)
        case f @ Frame.Header(ch, _, _, _) =>
          getChannel(ch).flatMap(_.recv(f)).flatMap(???)
        case Frame.Method(ch, m) => getChannel(ch).flatMap(handleMethod(_)(m))
        case Frame.Heartbeat     => sendQ.offer(Frame.Heartbeat)
      }
      .onFinalize(_status.set(Status.Closed))
      .drain

}
