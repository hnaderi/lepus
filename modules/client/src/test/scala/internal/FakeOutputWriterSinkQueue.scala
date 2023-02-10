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
import cats.effect.std.Queue
import cats.implicits.*
import lepus.protocol.Frame
import munit.CatsEffectAssertions.*
import munit.Location

/** Fake output to a queue This is for tests that require blocking behaviour of
  * a queue
  */
final class FakeOutputWriterSinkQueue(q: Queue[IO, Frame])
    extends OutputWriterSink[IO, Frame] {
  def write(t: Frame): IO[Unit] = q.offer(t)
  def assert(f: Frame)(using Location) = q.take.assertEquals(f)
  def assertEmpty(using Location) = size.assertEquals(0)
  export q.{size, take}
}
object FakeOutputWriterSinkQueue {
  def apply() = Queue.unbounded[IO, Frame].map(new FakeOutputWriterSinkQueue(_))
}
