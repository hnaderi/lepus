package lepus.core

import fs2.Stream

trait Channel[F[_]]
    extends BasicOperations[F]
    with ExchangeOperations[F]
    with QueueOperations[F]
    with TransactionOperations[F]
    with TerminateOperations[F] {

  /** Gives the channel number for this specific channel
    */
  def number: F[ChannelNumber]

  /** Gives access to [Connection] which this channel is using
    */
  def connection: F[Connection[F]]

  /** Stream that receives returned messages from broker */
  def returns: Stream[F, Unit]

  /** Stream that receives confirm messages from broker */
  def confirms: Stream[F, Unit]

  def confirmSelect: F[Unit]
  def nextPublishSeqNr: F[PublishSeqNr]
  def waitForConfirms: F[Boolean]
}
