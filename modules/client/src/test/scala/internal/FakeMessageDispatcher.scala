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
import cats.effect.std.UUIDGen
import cats.implicits.*
import lepus.protocol.domains.ConsumerTag
import lepus.protocol.domains.ShortString
import munit.Assertions.*
import munit.CatsEffectAssertions.*
import munit.Location

final class FakeMessageDispatcher(
    delivered: Ref[IO, List[DeliveredMessageRaw]],
    returns: Ref[IO, List[ReturnedMessageRaw]],
    confirms: Ref[IO, List[ConfirmationResponse]],
    cancelled: Ref[IO, List[ConsumerTag]],
    underlying: MessageDispatcher[IO]
) extends MessageDispatcher[IO] {

  def deliver(msg: DeliveredMessageRaw): IO[Unit] =
    delivered.update(_.prepended(msg)) >> underlying.deliver(msg)

  def cancel(ctag: ConsumerTag): IO[Unit] =
    cancelled.update(_.prepended(ctag)) >> underlying.cancel(ctag)

  def `return`(msg: ReturnedMessageRaw): IO[Unit] =
    returns.update(_.prepended(msg)) >> underlying.`return`(msg)

  export underlying.{deliveryQ, confirmationQ, returnQ}

  override def confirm(msg: ConfirmationResponse): IO[Unit] =
    confirms.update(_.prepended(msg)) >> underlying.confirm(msg)

  def assertDelivered(msg: DeliveredMessageRaw)(using Location): IO[Unit] =
    delivered.get.map(_.contains(msg)).assert
  def assertReturned(msg: ReturnedMessageRaw)(using Location): IO[Unit] =
    returns.get.map(_.contains(msg)).assert
  def assertConfirmed(msg: ConfirmationResponse)(using Location): IO[Unit] =
    confirms.get.map(_.contains(msg)).assert
  def assertCancelled(ctag: ConsumerTag)(using Location): IO[Unit] =
    cancelled.get.map(_.contains(ctag)).assert

  def assertNoDelivery(using Location): IO[Unit] =
    delivered.get.assertEquals(Nil)
  def assertNoReturn(using Location): IO[Unit] = returns.get.assertEquals(Nil)
  def assertNoContent(using Location): IO[Unit] =
    assertNoDelivery >> assertNoReturn
  def assertNoCancel(using Location): IO[Unit] =
    cancelled.get.assertEquals(Nil)
}

object FakeMessageDispatcher {
  def apply(): IO[FakeMessageDispatcher] =
    (
      IO.ref(List.empty[DeliveredMessageRaw]),
      IO.ref(List.empty[ReturnedMessageRaw]),
      IO.ref(List.empty[ConfirmationResponse]),
      IO.ref(List.empty[ConsumerTag]),
      MessageDispatcher[IO]()
    )
      .mapN(new FakeMessageDispatcher(_, _, _, _, _))
}
