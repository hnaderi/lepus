package lepus.client
package internal

import cats.effect.*
import cats.effect.implicits.*
import cats.effect.std.Queue
import cats.effect.std.QueueSource
import cats.implicits.*
import fs2.Pipe
import fs2.Stream
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import lepus.client.Connection.Status
import lepus.client.apis.*
import lepus.protocol.*
import lepus.protocol.constants.ErrorCode
import lepus.protocol.constants.ErrorType
import lepus.protocol.domains.ChannelNumber

import internal.*

private[client] trait ConnectionLowLevel[F[_]] {
  def onClosed: F[Unit]
  def onConnected(config: NegotiatedConfig): F[Unit]

  def addChannel[MC <: MessagingChannel](
      f: ChannelTransmitter[F] => Resource[F, Channel[F, MC]]
  ): Resource[F, Channel[F, MC]]

  def signal: Signal[F, Status]
  def channels: Signal[F, Set[ChannelNumber]]
}

// enum State[F[_]] {
//   case New(next: Deferred[F, State[F]])
//   case Open(handler: FrameDispatcher[F], mkCh: F[LowlevelChannel[F]])
//   case Closed()
// }
// object State {
//   def apply[F[_]](using F: Concurrent[F]): F[State[F]] =
//     F.deferred[State[F]].map(State.New(_))
// }
