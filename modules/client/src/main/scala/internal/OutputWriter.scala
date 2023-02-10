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
import cats.implicits.*

private[client] trait OutputWriterSink[F[_], T] {
  def write(t: T): F[Unit]
}

private[client] trait OutputWriter[F[_], T] extends OutputWriterSink[F, T] {
  def onClose: F[Unit]
}

private[client] object OutputWriter {
  def apply[F[_]: Concurrent, T](out: T => F[Unit]): F[OutputWriter[F, T]] =
    Deferred[F, ConnectionIsClosed.type]
      .map(closed =>
        new {
          override def write(t: T): F[Unit] =
            closed.tryGet.flatMap {
              case Some(_) => ConnectionIsClosed.raiseError
              case None =>
                Concurrent[F].race(closed.get, out(t)).flatMap(_.liftTo)
            }

          override def onClose: F[Unit] =
            closed.complete(ConnectionIsClosed).void
        }
      )

  case object ConnectionIsClosed
      extends RuntimeException("Connection is already closed!")
}
