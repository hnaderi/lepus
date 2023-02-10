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
import cats.implicits.*
import lepus.protocol.Frame
import munit.Location

/** Output writer to list
  */
final class FakeOutputWriterSink[T](data: InteractionList[T])
    extends OutputWriterSink[IO, T] {
  def write(t: T): IO[Unit] = data.add(t)
  def assert(f: T*)(using Location) = data.assertQ(f: _*)
  def assertEmpty(using Location) = data.assert()
  def size = data.size
  def all = data.all.map(_.reverse)
}

object FakeOutputWriterSink {
  def of[T] = InteractionList[T].map(new FakeOutputWriterSink(_))
  def apply() = of[Frame]
}
