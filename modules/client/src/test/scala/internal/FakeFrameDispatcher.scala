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
import cats.syntax.all.*
import fs2.concurrent.Signal
import lepus.protocol.*
import lepus.protocol.domains.ChannelNumber
import munit.CatsEffectAssertions.*
import munit.Location

import Frame.*

final class FakeFrameDispatcher(
    val dispatched: InteractionList[Frame],
    error: Option[Exception],
    closed: Ref[IO, Boolean]
) extends FrameDispatcher[IO] {

  override def onClose: IO[Unit] = closed.set(true)

  override def body(b: Body): IO[Unit] = dispatch(b)

  override def invoke(m: Frame.Method): IO[Unit] = dispatch(m)

  override def channels: Signal[IO, Set[ChannelNumber]] =
    Signal.constant(Set.empty)

  override def header(h: Header): IO[Unit] = dispatch(h)

  override def add[CHANNEL <: ChannelReceiver[IO]](
      build: ChannelNumber => Resource[IO, CHANNEL]
  ): Resource[IO, CHANNEL] = build(ChannelNumber(1))

  private def dispatch(frame: Frame) =
    dispatched.add(frame) >> error.fold(IO.unit)(IO.raiseError)

  def assertClosed: IO[Unit] = closed.get.assert
  def assertOpen: IO[Unit] = closed.get.map(!_).assert
}

object FakeFrameDispatcher {
  def apply(error: Option[Exception] = None): IO[FakeFrameDispatcher] =
    (InteractionList[Frame], IO.ref(false))
      .mapN(new FakeFrameDispatcher(_, error, _))
}
