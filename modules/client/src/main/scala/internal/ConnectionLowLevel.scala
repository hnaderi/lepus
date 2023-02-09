/*
 * Copyright 2021 Hossein Naderi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// package lepus.client
// package internal

// import cats.effect.*
// import cats.effect.implicits.*
// import cats.effect.kernel.DeferredSource
// import cats.effect.kernel.Resource.ExitCase.Canceled
// import cats.effect.kernel.Resource.ExitCase.Errored
// import cats.effect.kernel.Resource.ExitCase.Succeeded
// import cats.effect.std.Mutex
// import cats.effect.std.Queue
// import cats.effect.std.QueueSink
// import cats.effect.std.QueueSource
// import cats.implicits.*
// import fs2.Pipe
// import fs2.Stream
// import fs2.concurrent.Signal
// import fs2.concurrent.SignallingRef
// import lepus.client.Connection.Status
// import lepus.client.apis.*
// import lepus.protocol.*
// import lepus.protocol.constants.ReplyCategory
// import lepus.protocol.constants.ReplyCode
// import lepus.protocol.domains.*

// import scala.concurrent.duration.*

// private[client] trait ConnectionLowLevel[F[_]] {
//   def handler: Pipe[F, Frame, Nothing]

//   def newChannel: Resource[F, ChannelTransmitter[F]]

//   def signal: Signal[F, Connection.Status]
//   def channels: Signal[F, Set[ChannelNumber]]
// }

// private[client] object ConnectionLowLevel {
//   def apply[F[_]: Temporal](
//       config: F[NegotiatedConfig],
//       vhost: Path,
//       output: QueueSink[F, Frame],
//       buildChannel: ChannelFactory[F]
//   ): F[ConnectionLowLevel[F]] =
//     FrameDispatcher[F].flatMap(from(config, vhost, _, output, buildChannel))

//   def from[F[_]: Temporal](
//       config: F[NegotiatedConfig],
//       vhost: Path,
//       dispatcher: FrameDispatcher[F],
//       output: QueueSink[F, Frame],
//       buildChannel: ChannelFactory[F]
//   ): F[ConnectionLowLevel[F]] = for {
//     state <- SignallingRef[F].of(Status.Connecting)
//     so <- ChannelOutput(output, 10)
//     rpc <- RPCChannel(so, ChannelNumber(0), 10)
//     isClosed <- Deferred[F, Either[Throwable, Unit]]
//   } yield new {

//     private def heartbeats(config: NegotiatedConfig) =
//       Stream
//         .awakeEvery(config.heartbeat.toInt.seconds)
//         .foreach(_ => output.offer(Frame.Heartbeat))

//     private val closeConnection =
//       signal.get
//         .map(_ == Status.Opened)
//         .ifM(
//           rpc
//             .sendWait(
//               ConnectionClass.Close(
//                 ReplyCode.ReplySuccess,
//                 ShortString(""),
//                 ClassId(0),
//                 MethodId(0)
//               )
//             )
//             .void,
//           Concurrent[F].unit
//         )

//     private val initialization = {
//       import fs2.Stream.*
//       eval(config)
//         .flatMap(config =>
//           eval(state.set(Status.Connected)) *>
//             eval(rpc.sendWait(ConnectionClass.Open(vhost))) *>
//             eval(state.set(Status.Opened)) *>
//             heartbeats(config)
//         )
//         .onFinalize(closeConnection)
//         .interruptWhen(isClosed)
//     }

//     override def handler: Pipe[F, Frame, Nothing] = _.foreach {
//       case b: Frame.Body   => dispatcher.body(b)
//       case h: Frame.Header => dispatcher.header(h)
//       case Frame.Method(0, value) =>
//         value match {
//           case m @ ConnectionClass.OpenOk  => rpc.recv(m)
//           case m @ ConnectionClass.CloseOk => rpc.recv(m)
//           case _: ConnectionClass.Close =>
//             output.offer(
//               Frame.Method(ChannelNumber(0), ConnectionClass.CloseOk)
//             ) >> state.set(Status.Closed)
//           case _ => ???
//         }
//       case m: Frame.Method => dispatcher.invoke(m)
//       case Frame.Heartbeat => output.offer(Frame.Heartbeat)
//     }
//       .onFinalizeCase {
//         case Errored(e) => isClosed.complete(Left(e)).void
//         case _          => isClosed.complete(Right(())).void
//       }
//       .merge(initialization)
//       .onFinalize(state.set(Status.Closed))

//     import Channel.call
//     private def openChannel(ch: LowlevelChannel[F]) =
//       Resource.make(ch.call(ChannelClass.Open).void)(_ =>
//         // ch.status.get
//         //   .map(_ == Channel.Status.Closed)
//         //   .ifM(
//         //     Concurrent[F].unit,
//         ch.call(
//           ChannelClass.Close(
//             ReplyCode.ReplySuccess,
//             ShortString(""),
//             ClassId(0),
//             MethodId(0)
//           )
//         ).void
//       )
//       // )

//     private def waitTilOpened = signal.discrete
//       .flatMap {
//         case Status.Opened => Stream.unit
//         case Status.Closed =>
//           Stream.raiseError[F](new Exception("Connection failed"))
//         case _ => Stream.empty
//       }
//       .head
//       .compile
//       .drain
//       .toResource

//     override def newChannel: Resource[F, ChannelTransmitter[F]] = for {
//       _ <- waitTilOpened
//       ch <- dispatcher.add(n =>
//         buildChannel(ChannelBuildInput(n, output)).toResource
//       )
//       _ <- openChannel(ch)
//     } yield ch

//     override def signal: Signal[F, Status] = state

//     override def channels: Signal[F, Set[ChannelNumber]] = dispatcher.channels

//   }
// }
