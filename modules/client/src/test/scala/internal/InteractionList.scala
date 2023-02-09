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
import cats.syntax.all.*
import munit.CatsEffectAssertions.*
import munit.Location

final class InteractionList[T](interactions: Ref[IO, List[T]]) extends AnyVal {
  def add(value: T) = interactions.update(value :: _)

  def reset: IO[Unit] = interactions.set(Nil)

  def assert(vs: T*)(using Location): IO[Unit] =
    interactions.get.assertEquals(vs.toList)

  def assertContains(vs: T*)(using Location): IO[Unit] =
    interactions.get.map(list => vs.forall(v => list.contains(v))).assert

  def assertContainsSlice(vs: T*)(using Location): IO[Unit] =
    interactions.get.map(_.containsSlice(vs)).assert

  def assertLast(v: T)(using Location) = last.assertEquals(Some(v))
  def assertFirst(v: T)(using Location) = first.assertEquals(Some(v))

  def all: IO[List[T]] = interactions.get
  def last = all.map(_.headOption)
  def first = all.map(_.lastOption)
}

object InteractionList {
  def apply[T]: IO[InteractionList[T]] =
    IO.ref(List.empty[T]).map(new InteractionList(_))
}
