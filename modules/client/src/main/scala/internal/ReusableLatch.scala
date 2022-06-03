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

import cats.effect.Concurrent
import cats.effect.kernel.Deferred
import cats.effect.kernel.Ref
import cats.implicits.*

private[client] final class ReusableLatch[F[_]](r: Ref[F, Deferred[F, Unit]])(
    using F: Concurrent[F]
) {
  def run[T](f: F[T]): F[T] = r.get.flatMap(_.get) >> f
  def block: F[Unit] = Deferred[F, Unit].flatMap(r.set)
  def unblock: F[Unit] = r.get.flatMap(_.complete(()).void)
}

private[client] object ReusableLatch {
  def apply[F[_]: Concurrent]: F[ReusableLatch[F]] = for {
    d <- Deferred[F, Unit].flatTap(_.complete(()))
    r <- Ref.of(d)
  } yield new ReusableLatch(r)
}
