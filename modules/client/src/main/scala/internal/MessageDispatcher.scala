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
import cats.implicits.*
import fs2.Stream
import lepus.protocol.domains.ConsumerTag

private[client] trait MessageDispatcher[F[_]] {
  def deliver(msg: DeliveredMessage): F[Unit]
  def `return`(msg: ReturnedMessage): F[Unit]
  def deliveryQ(
      ctag: ConsumerTag
  ): Resource[F, QueueSource[F, DeliveredMessage]]
  def returnQ: QueueSource[F, ReturnedMessage]
}

object MessageDispatcher {
  def apply[F[_]](using F: Concurrent[F]): F[MessageDispatcher[F]] = for {
    dqs <- F.ref(Map.empty[ConsumerTag, Queue[F, DeliveredMessage]])
    rq <- Queue.bounded[F, ReturnedMessage](1) // TODO queue size
  } yield new {
    def deliver(msg: DeliveredMessage): F[Unit] =
      dqs.get.map(_.get(msg.consumerTag)).flatMap {
        case Some(q) => q.offer(msg)
        case None    => F.unit
      }
    def `return`(msg: ReturnedMessage): F[Unit] = rq.offer(msg)
    def deliveryQ(
        ctag: ConsumerTag
    ): Resource[F, QueueSource[F, DeliveredMessage]] =
      Resource
        .eval(dqs.modify { m =>
          val f = m
            .get(ctag)
            .fold(addQ(ctag))(_ =>
              Resource.eval(AlreadyExists(ctag).raiseError)
            )

          (m, f)
        })
        .flatten

    private def addQ(
        ctag: ConsumerTag
    ): Resource[F, QueueSource[F, DeliveredMessage]] = Resource.make(
      Queue
        .bounded[F, DeliveredMessage](1) // TODO queue size
        .flatTap(q => dqs.update(_.updated(ctag, q)))
    )(_ => removeQ(ctag))

    private def removeQ(ctag: ConsumerTag) = dqs.update(_ - ctag)

    def returnQ: QueueSource[F, ReturnedMessage] = rq
  }

  final case class AlreadyExists(ctag: ConsumerTag) extends Throwable
}
