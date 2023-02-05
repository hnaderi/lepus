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
import cats.effect.kernel.DeferredSource
import cats.effect.kernel.Ref
import cats.syntax.all.*
import lepus.client.SynchronousGet
import lepus.client.internal.FakeContentChannel.Interaction
import lepus.protocol.BasicClass
import lepus.protocol.Frame
import munit.CatsEffectAssertions.*

import Frame.*

final class FakeContentChannel(
    interactions: Ref[IO, List[Interaction]],
    val error: PlannedError
) extends ContentChannel[IO] {

  override def asyncNotify(m: ContentMethod): IO[Unit] = interact(
    Interaction.AsyncNotify(m)
  )

  override def recv(h: Header | Body): IO[Unit] = interact(Interaction.Recv(h))

  override def syncNotify(m: ContentSyncResponse): IO[Unit] = interact(
    Interaction.SyncNotify(m)
  )

  override def get(
      m: BasicClass.Get
  ): IO[DeferredSource[IO, Option[SynchronousGet]]] =
    interact(Interaction.Get(m)) >> IO.deferred.flatTap(_.complete(None))

  override def abort: IO[Unit] = interact(Interaction.Abort)

  private def interact(value: Interaction) =
    interactions.update(value :: _) >> error.run

  def assert(values: Interaction*): IO[Unit] =
    interactions.get.assertEquals(values.toList)
}

object FakeContentChannel {
  enum Interaction {
    case AsyncNotify(m: ContentMethod)
    case Recv(h: Header | Body)
    case SyncNotify(m: ContentSyncResponse)
    case Get(m: BasicClass.Get)
    case Abort
  }
  def apply(): IO[FakeContentChannel] =
    (IO.ref(List.empty[Interaction]), PlannedError())
      .mapN(new FakeContentChannel(_, _))
}
