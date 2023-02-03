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
package internal

import cats.effect.Concurrent
import cats.effect.std.Queue
import cats.implicits.*
import fs2.Stream
import lepus.protocol.*
import lepus.protocol.constants.ErrorCode
import lepus.protocol.domains.ConsumerTag
import lepus.protocol.domains.ChannelNumber
import cats.effect.std.QueueSink

type ContentMethod = BasicClass.Deliver | BasicClass.Return
type ContentSyncResponse = BasicClass.GetOk | BasicClass.GetEmpty.type

private[client] trait ChannelReceiver[F[_]] {
  def asyncContent(m: ContentMethod): F[Unit | ErrorCode]
  def syncContent(m: ContentSyncResponse): F[Unit | ErrorCode]
  def header(h: Frame.Header): F[Unit | ErrorCode]
  def body(h: Frame.Body): F[Unit | ErrorCode]
  def method(m: Method): F[Unit | ErrorCode]
}

private[client] trait ChannelTransmitter[F[_]] {
  def publish(method: BasicClass.Publish, msg: Message): F[Unit]

  def sendWait(m: Method): F[Method]
  def sendNoWait(m: Method): F[Unit]

  def get(m: BasicClass.Get): F[Option[SynchronousGet]]

  def delivered(ctag: ConsumerTag): Stream[F, DeliveredMessage]
  def returned: Stream[F, ReturnedMessage]
}

private[client] trait LowlevelChannel[F[_]]
    extends ChannelReceiver[F],
      ChannelTransmitter[F]

/** Facade over other small components */
private[client] object LowlevelChannel {
  def from[F[_]: Concurrent](
      channelNumber: ChannelNumber,
      disp: MessageDispatcher[F],
      sendQ: QueueSink[F, Frame]
  ): F[LowlevelChannel[F]] = for {
    out <- ChannelOutput(sendQ)
    wlist <- Waitlist[F, Option[SynchronousGet]]()
    content <- ContentChannel(channelNumber, out, disp, wlist)
    rpc <- RPCChannel(out, channelNumber, 10)
    pub = ChannelPublisher(channelNumber, 100, out)
    ch <- apply(content, rpc, pub, disp, out)
  } yield ch

  def apply[F[_]: Concurrent](
      content: ContentChannel[F],
      rpc: RPCChannel[F],
      pub: ChannelPublisher[F],
      disp: MessageDispatcher[F],
      out: ChannelOutput[F, Frame]
  ): F[LowlevelChannel[F]] = for {
    _ <- Queue.bounded[F, Frame](10)
  } yield new LowlevelChannel[F] {
    def asyncContent(m: ContentMethod): F[Unit | ErrorCode] =
      content.asyncNotify(m)
    def syncContent(m: ContentSyncResponse): F[Unit | ErrorCode] =
      content.syncNotify(m)
    def header(h: Frame.Header): F[Unit | ErrorCode] = content.recv(h)
    def body(h: Frame.Body): F[Unit | ErrorCode] = content.recv(h)
    def method(m: Method): F[Unit | ErrorCode] =
      // TODO match based on method
      m match {
        case ChannelClass.Flow(e) => out.block.widen
        case _                    => content.abort >> rpc.recv(m)
      }

    def publish(method: BasicClass.Publish, msg: Message): F[Unit] =
      pub.send(method, msg)

    def sendWait(m: Method): F[Method] = rpc.sendWait(m)
    def sendNoWait(m: Method): F[Unit] = rpc.sendNoWait(m)
    def get(m: BasicClass.Get): F[Option[SynchronousGet]] =
      content.get(m).flatMap(_.get)
    def delivered(ctag: ConsumerTag): Stream[F, DeliveredMessage] = Stream
      .resource(disp.deliveryQ(ctag))
      .flatMap(Stream.fromQueueUnterminated(_, 100))
    def returned: Stream[F, ReturnedMessage] =
      Stream.fromQueueUnterminated(disp.returnQ, 100)
  }

}
