package lepus.std

import fs2.Stream

trait WorkPoolServer[F[_], T] {
  def publish(t: T): F[Unit]
  def responses: Stream[F, Int]
}
