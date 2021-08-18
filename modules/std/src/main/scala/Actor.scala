package lepus.std

import fs2.Stream

trait Actor[F[_], T] {
  def inbox: Stream[F, T]
  def accept(): F[Unit]
  def reject(): F[Unit]
  def outbox(): F[Unit]
}
