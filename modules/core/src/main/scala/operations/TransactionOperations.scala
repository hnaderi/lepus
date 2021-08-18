package lepus.core

import fs2.Stream

trait TransactionOperations[F[_]] {
  def txBegin: F[Unit]
  def txCommit: F[Unit]
  def txRollback: F[Unit]
}
