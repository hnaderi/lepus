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
import cats.effect.kernel.Resource
import cats.implicits.*
import fs2.Stream
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import lepus.client.Channel.Status
import lepus.protocol.ChannelClass.Close
import lepus.protocol.*
import lepus.protocol.domains.ConsumerTag

private[client] type ContentMethod = BasicClass.Deliver | BasicClass.Return
private[client] type ContentSyncResponse =
  BasicClass.GetOk | BasicClass.GetEmpty.type
private[client] type ConfirmationResponse =
  BasicClass.Ack | BasicClass.Nack

private[client] trait ChannelReceiver[F[_]] {
  def asyncContent(m: ContentMethod): F[Unit]
  def syncContent(m: ContentSyncResponse): F[Unit]
  def header(h: Frame.Header): F[Unit]
  def body(h: Frame.Body): F[Unit]
  def method(m: Method): F[Unit]
  def onClose: F[Unit]
}

private[client] trait ChannelTransmitter[F[_]] {
  def publish(method: BasicClass.Publish, msg: MessageRaw): F[Unit]

  def sendWait(m: Method): F[Method]
  def sendNoWait(m: Method): F[Unit]

  def get(m: BasicClass.Get): F[Option[SynchronousGetRaw]]

  def delivered(ctag: Option[ConsumerTag]): Resource[F, (ConsumerTag, Stream[F, DeliveredMessageRaw])]
  final def delivered: Resource[F, (ConsumerTag, Stream[F, DeliveredMessageRaw])] =
    delivered(None)

  def returned: Stream[F, ReturnedMessageRaw]
  def confirmed: Stream[F, Confirmation]

  def status: Signal[F, Status]
}

private[client] trait LowlevelChannel[F[_]]
    extends ChannelReceiver[F],
      ChannelTransmitter[F]

/** Facade over other small components */
private[client] object LowlevelChannel {
  def from[F[_]: Concurrent](config: ChannelConfig): ChannelFactory[F] = in =>
    for {
      disp <- MessageDispatcher[F](
        returnedBufSize = config.returnedBufSize,
        confirmBufSize = config.confirmBufSize,
        deliveryBufSize = config.deliveryBufSize
      )
      out <- ChannelOutput(in.output, maxMethods = config.maxConcurrentPublish)
      wlist <- Waitlist[F, Option[SynchronousGetRaw]](size =
        config.maxConcurrentGet
      )
      content <- ContentChannel(in.number, out, disp, wlist)
      rpc <- RPCChannel(out, in.number, maxMethods = config.maxConcurrentRPC)
      pub = ChannelPublisher(in.number, in.frameMax, out)
      ch <- apply(content, rpc, pub, disp, out)
    } yield ch

  def apply[F[_]](
      content: ContentChannel[F],
      rpc: RPCChannel[F],
      pub: ChannelPublisher[F],
      disp: MessageDispatcher[F],
      out: ChannelOutput[F, Frame]
  )(using F: Concurrent[F]): F[LowlevelChannel[F]] = for {
    state <- SignallingRef[F].of(Status.Active)
  } yield new LowlevelChannel[F] {

    override def onClose: F[Unit] =
      state.set(Status.Closed) >> rpc.sendNoWait(ChannelClass.CloseOk)

    private def handle(f: F[Unit]) =
      f.onError(_ => state.set(Status.Closed)).recoverWith {
        case e: AMQPError if e.replyCode.isChannelError =>
          rpc.sendNoWait(
            ChannelClass.Close(
              e.replyCode,
              e.replyText,
              e.classId,
              e.methodId
            )
          )
      }

    override def status: Signal[F, Status] = state

    private def isClosed = status.map(_ == Status.Closed)

    private def setFlow(e: Boolean) =
      if e
      then out.unblock >> state.set(Status.Active)
      else out.block >> state.set(Status.InActive)

    def asyncContent(m: ContentMethod): F[Unit] =
      handle(content.asyncNotify(m))

    def syncContent(m: ContentSyncResponse): F[Unit] =
      handle(content.syncNotify(m))

    def header(h: Frame.Header): F[Unit] = handle(content.recv(h))

    def body(h: Frame.Body): F[Unit] = handle(content.recv(h))

    def method(m: Method): F[Unit] =
      handle(m match {
        case ChannelClass.Flow(e) =>
          setFlow(e) >> rpc.sendNoWait(ChannelClass.FlowOk(e))
        case ChannelClass.Close(replyCode, replyText, classId, methodId) =>
          onClose
        case m @ ChannelClass.CloseOk => state.set(Status.Closed) >> rpc.recv(m)
        case m: ConfirmationResponse  => disp.confirm(m)
        case BasicClass.Cancel(ctag, _) => disp.cancel(ctag)
        case _                          => content.abort >> rpc.recv(m)
      })

    def publish(method: BasicClass.Publish, msg: MessageRaw): F[Unit] =
      pub.send(method, msg)

    def sendWait(m: Method): F[Method] =
      F.race(rpc.sendWait(m), isClosed.waitUntil(identity)).flatMap {
        case Right(_)    => F.raiseError(ChannelIsClosed)
        case Left(value) => F.pure(value)
      }

    def sendNoWait(m: Method): F[Unit] = rpc.sendNoWait(m)
    def get(m: BasicClass.Get): F[Option[SynchronousGetRaw]] =
      content.get(m).flatMap(_.get)

    def delivered(ctag: Option[ConsumerTag]): Resource[F, (ConsumerTag, Stream[F, DeliveredMessageRaw])] =
      disp.deliveryQ(ctag).map { case (ctag, q) =>
        (ctag, Stream.fromQueueNoneTerminated(q).interruptWhen(isClosed))
      }

    def returned: Stream[F, ReturnedMessageRaw] =
      Stream.fromQueueUnterminated(disp.returnQ).interruptWhen(isClosed)

    def confirmed: Stream[F, Confirmation] = Stream
      .fromQueueUnterminated(disp.confirmationQ)
      .map {
        case BasicClass.Ack(tag, multi) =>
          Confirmation(Acknowledgment.Ack, tag, multi)
        case BasicClass.Nack(tag, multi, _) =>
          Confirmation(Acknowledgment.Nack, tag, multi)
      }
      .interruptWhen(isClosed)
  }

  case object ChannelIsClosed
      extends RuntimeException("Channel is already closed!")
}
