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

final class PlannedError(value: Ref[IO, PlannedErrorKind]) extends AnyVal {
  def run = value.get.flatMap {
    case PlannedErrorKind.Always(e) => IO.raiseError(e)
    case PlannedErrorKind.Times(n, e) if n > 0 =>
      value.set(PlannedErrorKind.Times(n - 1, e)) >> IO.raiseError(e)
    case _ => IO.unit
  }

  def set(kind: PlannedErrorKind) = value.set(kind)
}

enum PlannedErrorKind {
  case Never
  case Always(e: Exception)
  case Times(n: Int, e: Exception)
}

object PlannedError {
  def apply(): IO[PlannedError] =
    IO.ref(PlannedErrorKind.Never).map(new PlannedError(_))
}
