package lepus.core

import fs2.Stream

trait TerminateOperations[F[_]] {
  def close: F[Unit]
  def close(code: Int, message: String): F[Unit]
  def abort: F[Unit]
  def abort(code: Int, message: String): F[Unit]
}
