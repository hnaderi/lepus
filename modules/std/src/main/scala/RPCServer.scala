package lepus.std

import fs2.Stream

trait RPCServer[F[_], T] {
  def requests: Stream[F, T]
  def respond(): F[Unit]
}
