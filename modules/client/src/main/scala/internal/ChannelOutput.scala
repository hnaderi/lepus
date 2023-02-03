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

private[client] trait SequentialOutput[F[_], T] {
  def writeOne(f: T): F[Unit]
  def writeAll(fs: T*): F[Unit]
}

private[client] trait ChannelOutput[F[_], T] extends SequentialOutput[F, T] {
  def block: F[Unit]
  def unblock: F[Unit]
}

private[client] object ChannelOutput {
  def apply[F[_], T](
      q: QueueSink[F, T],
      maxMethods: Int = 10
  )(using F: Concurrent[F]): F[ChannelOutput[F, T]] =
    for {
      sem <- Semaphore[F](maxMethods)
      writer = Resource.makeFull[F, Unit](poll =>
        poll(sem.acquireN(maxMethods))
      )(_ => sem.releaseN(maxMethods))
      lock <- ReusableLatch[F]
    } yield new {

      def writeOne(f: T): F[Unit] = lock.run(sem.permit.use(_ => q.offer(f)))

      def writeAll(fs: T*): F[Unit] =
        lock.run(writer.use(_ => fs.traverse(q.offer).void))

      def block: F[Unit] = lock.block
      def unblock: F[Unit] = lock.unblock
    }
}
