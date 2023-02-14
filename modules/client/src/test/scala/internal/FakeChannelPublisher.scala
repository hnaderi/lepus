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
import lepus.client.MessageRaw
import lepus.protocol.BasicClass.Publish
import munit.CatsEffectAssertions.*

final class FakeChannelPublisher(sent: Ref[IO, List[(Publish, MessageRaw)]])
    extends ChannelPublisher[IO] {

  override def send(method: Publish, msg: MessageRaw): IO[Unit] =
    sent.update((method, msg) :: _)

  def assert(values: (Publish, MessageRaw)*): IO[Unit] =
    sent.get.assertEquals(values.toList)
}

object FakeChannelPublisher {
  def apply() =
    IO.ref(List.empty[(Publish, MessageRaw)]).map(new FakeChannelPublisher(_))
}
