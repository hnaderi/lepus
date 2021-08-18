package lepus.std

import fs2.Stream

trait WorkPoolClient[F[_], T] {
  def jobs: Stream[F, T]
  def respond(): F[Unit]
}
