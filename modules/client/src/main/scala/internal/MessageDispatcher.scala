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
import cats.effect.std.*
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.Stream
import lepus.protocol.domains.*
import lepus.protocol.constants.ReplyCode

private[client] trait MessageDispatcher[F[_]] {
  def deliver(msg: DeliveredMessageRaw): F[Unit]
  def `return`(msg: ReturnedMessageRaw): F[Unit]
  def confirm(msg: ConfirmationResponse): F[Unit]

  def deliveryQ: Resource[F, (ConsumerTag, QueueSource[F, DeliveredMessageRaw])]
  def returnQ: QueueSource[F, ReturnedMessageRaw]
  def confirmationQ: QueueSource[F, ConfirmationResponse]
}

private[client] object MessageDispatcher {
  def apply[F[_]](
      returnedBufSize: Int = 10,
      confirmBufSize: Int = 10,
      deliveryBufSize: Int = 10
  )(using F: Concurrent[F]): F[MessageDispatcher[F]] = for {
    dqs <- F.ref(Map.empty[ConsumerTag, Queue[F, DeliveredMessageRaw]])
    rq <- Queue.bounded[F, ReturnedMessageRaw](returnedBufSize)
    cq <- Queue.bounded[F, ConfirmationResponse](confirmBufSize)
    counter <- F.ref(0)
  } yield new {

    private def newCtag = counter
      .getAndUpdate(_ + 1)
      .map(i => ConsumerTag.from(s"consumer-$i").getOrElse(???))
      .toResource

    override def deliver(msg: DeliveredMessageRaw): F[Unit] =
      dqs.get.map(_.get(msg.consumerTag)).flatMap {
        case Some(q) => q.offer(msg)
        case None    => F.unit
      }

    override def `return`(msg: ReturnedMessageRaw): F[Unit] = rq.offer(msg)

    override def deliveryQ
        : Resource[F, (ConsumerTag, QueueSource[F, DeliveredMessageRaw])] =
      for {
        ctag <- newCtag
        q <- addQ(ctag)
      } yield (ctag, q)

    private def addQ(
        ctag: ConsumerTag
    ): Resource[F, QueueSource[F, DeliveredMessageRaw]] = Resource.make(
      Queue
        .bounded[F, DeliveredMessageRaw](deliveryBufSize)
        .flatTap(q => dqs.update(_.updated(ctag, q)))
    )(_ => removeQ(ctag))

    private def removeQ(ctag: ConsumerTag) = dqs.update(_ - ctag)

    override def returnQ: QueueSource[F, ReturnedMessageRaw] = rq

    override def confirm(msg: ConfirmationResponse): F[Unit] = cq.offer(msg)

    override def confirmationQ: QueueSource[F, ConfirmationResponse] = cq
  }
}
