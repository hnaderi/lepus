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
import cats.effect.kernel.Resource
import cats.effect.std.QueueSink
import cats.effect.std.Semaphore
import cats.implicits.*
import lepus.protocol.Frame

private[client] trait SequentialOutput[F[_], T] {
  def writeOne(f: T): F[Unit]
  def writeAll(fs: T*): F[Unit]
}

private[client] object SequentialOutput {
  def apply[F[_]: Concurrent, T](
      q: QueueSink[F, T],
      maxMethods: Int = 10
  ): F[SequentialOutput[F, T]] =
    for {
      sem <- Semaphore[F](maxMethods)
      writer = Resource.makeFull[F, Unit](poll =>
        poll(sem.acquireN(maxMethods))
      )(_ => sem.releaseN(maxMethods))

    } yield new {
      def writeOne(f: T): F[Unit] = sem.permit.use(_ => q.offer(f))

      def writeAll(fs: T*): F[Unit] =
        writer.use(_ => fs.traverse(q.offer).void)
    }
}
