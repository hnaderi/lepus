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

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.effect.std.Queue
import cats.effect.std.QueueSource
import cats.implicits.*
import lepus.protocol.domains.ConsumerTag
import lepus.protocol.domains.ShortString
import munit.Assertions.*
import cats.effect.std.UUIDGen

final class FakeMessageDispatcher(
    delivered: Ref[IO, List[DeliveredMessage]],
    returns: Ref[IO, List[ReturnedMessage]],
    returned: Queue[IO, ReturnedMessage],
    deliveries: Ref[IO, Map[ConsumerTag, Queue[IO, DeliveredMessage]]]
) extends MessageDispatcher[IO] {
  def deliver(msg: DeliveredMessage): IO[Unit] =
    delivered.update(_.prepended(msg)) >> deliveries.get.flatMap(
      _.get(msg.consumerTag).fold(IO.unit)(_.offer(msg))
    )

  def `return`(msg: ReturnedMessage): IO[Unit] =
    returns.update(_.prepended(msg)) >> returned.offer(msg)

  private val newCtag = UUIDGen.randomString[IO].map(ShortString.from).flatMap {
    case Right(value) => IO(ConsumerTag(value))
    case Left(value)  => IO.raiseError(new RuntimeException(value))
  }

  def deliveryQ
      : Resource[IO, (ConsumerTag, QueueSource[IO, DeliveredMessage])] =
    Resource
      .eval(newCtag)
      .flatMap(ctag =>
        Resource.make(
          Queue
            .unbounded[IO, DeliveredMessage]
            .flatTap(q => deliveries.update(_.updated(ctag, q)))
            .map((ctag, _))
        )(_ => deliveries.update(_ - ctag))
      )

  def returnQ: QueueSource[IO, ReturnedMessage] = returned

  def assertDelivered(msg: DeliveredMessage): IO[Unit] =
    delivered.get.map(l => assert(l.contains(msg)))
  def assertReturned(msg: ReturnedMessage): IO[Unit] =
    returns.get.map(l => assert(l.contains(msg)))
  def assertNoDelivery: IO[Unit] = delivered.get.map(assertEquals(_, Nil))
  def assertNoReturn: IO[Unit] = returns.get.map(assertEquals(_, Nil))
  def assertNoContent: IO[Unit] = assertNoDelivery >> assertNoReturn

}

object FakeMessageDispatcher {
  def apply(): IO[FakeMessageDispatcher] =
    (
      IO.ref(List.empty[DeliveredMessage]),
      IO.ref(List.empty[ReturnedMessage]),
      Queue.unbounded[IO, ReturnedMessage],
      IO.ref(Map.empty[ConsumerTag, Queue[IO, DeliveredMessage]])
    )
      .mapN(new FakeMessageDispatcher(_, _, _, _))
}
