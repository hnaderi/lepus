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

import cats.Functor
import cats.effect.Concurrent
import cats.implicits.*
import fs2.Pipe
import fs2.Pull
import fs2.RaiseThrowable
import fs2.Stream

private[internal] object Helpers {

  type Step[F[_], T] = T => Stream[F, T]
  extension [F[_], T](self: Stream[F, T]) {
    def stagedRun(ss: Step[F, T]*): Stream[F, T] =
      def go(s: Stream[F, T])(ss: List[Step[F, T]]): Pull[F, T, Unit] =
        ss match {
          case head :: tail =>
            s.pull.uncons1.flatMap {
              case Some((t, next)) => head(t).pull.echo >> go(next)(tail)
              case None            => Pull.done
            }
          case Nil => s.pull.echo
        }

      go(self)(ss.toList).stream

    def run(p: Puller[F, T, ?]): Stream[F, T] = p.run(self).flatMap(_._2)
    def unconsStream: Stream[F, (T, Stream[F, T])] =
      self.pull.uncons1.flatMap(Pull.outputOption1).stream
  }

  type PullerProg[F[_], A, B] = Stream[F, A] => Stream[F, (B, Stream[F, A])]
  final case class Puller[F[_], A, +B](run: PullerProg[F, A, B])
      extends AnyVal {
    def flatMap[BB](f: B => Puller[F, A, BB]): Puller[F, A, BB] =
      Puller(stream => run(stream).flatMap((a, next) => f(a).run(next)))

    def map[BB](f: B => BB): Puller[F, A, BB] =
      Puller(run(_).map((b, n) => (f(b), n)))

    def eval(f: B => F[Unit])(using Functor[F]): Puller[F, A, B] =
      Puller(run(_).evalTap((a, n) => f(a)))

    def evalMap[BB](f: B => F[BB])(using Functor[F]): Puller[F, A, BB] =
      Puller(run(_).evalMap((a, n) => f(a).map((_, n))))

    def void: Puller[F, A, Unit] = map(_ => ())
  }

  object Puller {
    def pull[F[_], T]: Puller[F, T, T] = Puller(_.unconsStream)
  }
}
