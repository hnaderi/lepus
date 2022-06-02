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
import munit.Assertions.*

final class FakeMessageDispatcher(
    delivered: Ref[IO, List[DeliveredMessage]],
    returns: Ref[IO, List[ReturnedMessage]]
) extends MessageDispatcher[IO] {
  def deliver(msg: DeliveredMessage): IO[Unit] =
    delivered.update(_.prepended(msg))
  def `return`(msg: ReturnedMessage): IO[Unit] =
    returns.update(_.prepended(msg))
  def deliveryQ(
      ctag: ConsumerTag
  ): Resource[IO, QueueSource[IO, DeliveredMessage]] = ???
  def returnQ: QueueSource[IO, ReturnedMessage] = ???

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
    (IO.ref(List.empty[DeliveredMessage]), IO.ref(List.empty[ReturnedMessage]))
      .mapN(new FakeMessageDispatcher(_, _))
}
