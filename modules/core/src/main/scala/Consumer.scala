package lepus.core

import fs2.Stream

trait Consumer[F[_]] {
  def incoming: Stream[F, ConsumerEvent]
  def cancel: F[Unit]
}
