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
import lepus.client.internal.FakeChannelOutput.Interaction
import lepus.protocol.Frame
import munit.CatsEffectAssertions.*

final class FakeChannelOutput(interactions: Ref[IO, List[Interaction]])
    extends ChannelOutput[IO, Frame] {

  override def writeOne(f: Frame): IO[Unit] = interact(Interaction.WriteOne(f))

  override def writeAll(fs: Frame*): IO[Unit] = interact(
    Interaction.WriteAll(fs: _*)
  )

  override def block: IO[Unit] = interact(Interaction.Block)

  override def unblock: IO[Unit] = interact(Interaction.UnBlock)

  private def interact(value: Interaction) = interactions.update(value :: _)

  def assert(values: Interaction*): IO[Unit] =
    interactions.get.assertEquals(values.toList)
}

object FakeChannelOutput {
  enum Interaction {
    case WriteOne(frame: Frame)
    case WriteAll(frames: Frame*)
    case Block, UnBlock
  }
  def apply() = IO.ref(List.empty[Interaction]).map(new FakeChannelOutput(_))
}
