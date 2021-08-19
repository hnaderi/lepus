package lepus.client

import lepus.protocol.frame.*
import cats.effect.Concurrent
import cats.effect.Resource
import fs2.Stream
import fs2.Pipe

trait Channel[F[_]] {
  def consume: Stream[F, Byte]
  def publish: Pipe[F, Byte, Nothing]
  def deliveries: Stream[F, Unit]
  def returns: Stream[F, Unit]
}
