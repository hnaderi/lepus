package lepus.std

import fs2.Stream

trait RPCClient[F[_], T] {
  def call(t: T): F[Int]
}
