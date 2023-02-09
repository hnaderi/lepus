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
import lepus.client.internal.FakeFrameOutput.Interaction
import lepus.client.internal.OutputWriter
import lepus.protocol.Frame

final class FakeFrameOutput(val data: InteractionList[Interaction])
    extends OutputWriter[IO, Frame] {

  override def write(t: Frame): IO[Unit] = data.add(Interaction.Wrote(t))

  override def onClose: IO[Unit] = data.add(Interaction.Closed)

}

object FakeFrameOutput {
  enum Interaction {
    case Wrote(f: Frame)
    case Closed
  }
}
