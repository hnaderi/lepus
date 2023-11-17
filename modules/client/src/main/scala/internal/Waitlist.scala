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

package lepus.client
package internal

import cats.effect.Concurrent
import cats.effect.kernel.Deferred
import cats.effect.kernel.DeferredSource
import cats.effect.std.Queue
import cats.effect.std.Semaphore
import cats.implicits.*

private[client] trait Waitlist[F[_], T] {
  def checkinAnd(eval: F[Unit]): F[DeferredSource[F, T]]
  def nextTurn(t: T): F[Boolean]
}

private[client] object Waitlist {
  def apply[F[_]: Concurrent, T](size: Int = 100): F[Waitlist[F, T]] = for {
    l <- Queue.bounded[F, Deferred[F, T]](size)
    sem <- Semaphore[F](1)
  } yield new {
    def checkinAnd(eval: F[Unit]): F[DeferredSource[F, T]] = for {
      d <- Deferred[F, T]
      _ <- sem.permit.use(_ => l.offer(d) >> eval)
    } yield d

    def nextTurn(t: T): F[Boolean] = l.tryTake.flatMap {
      case Some(d) => d.complete(t)
      case None    => false.pure
    }
  }
}
