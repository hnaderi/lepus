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

package lepus.client.internal

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.implicits.*
import lepus.client.internal.FakeReceiver.Interaction
import lepus.codecs.FrameGenerators
import lepus.protocol.*
import lepus.protocol.classes.basic.Properties
import lepus.protocol.constants.ErrorCode
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*
import munit.CatsEffectSuite
import munit.ScalaCheckSuite
import org.scalacheck.effect.PropF.forAllF
import scodec.bits.ByteVector

final class FakeReceiver(
    interactionList: Ref[IO, List[FakeReceiver.Interaction]],
    error: Ref[IO, Option[ErrorCode]]
) extends ChannelReceiver[IO] {
  def asyncContent(m: ContentMethod): IO[Unit | ErrorCode] = interact(
    Interaction.AsyncContent(m)
  )
  def syncContent(m: ContentSyncResponse): IO[Unit | ErrorCode] = interact(
    Interaction.SyncContent(m)
  )
  def header(h: Frame.Header): IO[Unit | ErrorCode] = interact(
    Interaction.Header(h)
  )
  def body(h: Frame.Body): IO[Unit | ErrorCode] = interact(Interaction.Body(h))
  def method(m: Method): IO[Unit | ErrorCode] = interact(Interaction.Method(m))

  private def interact(i: Interaction): IO[Unit | ErrorCode] =
    interactionList.update(_.prepended(i)) >> error.get.map(_.getOrElse(()))

  def setError(ec: ErrorCode): IO[Unit] = error.set(ec.some)
  def clearError: IO[Unit] = error.set(None)

  def interactions: IO[List[Interaction]] = interactionList.get
  def lastInteraction: IO[Option[Interaction]] = interactions.map(_.headOption)
}

object FakeReceiver {
  enum Interaction {
    case AsyncContent(m: ContentMethod)
    case SyncContent(m: ContentSyncResponse)
    case Header(h: Frame.Header)
    case Body(h: Frame.Body)
    case Method(m: lepus.protocol.Method)
  }
  def apply() =
    (IO.ref(List.empty[Interaction]), IO.ref(Option.empty[ErrorCode]))
      .mapN(new FakeReceiver(_, _))
}
