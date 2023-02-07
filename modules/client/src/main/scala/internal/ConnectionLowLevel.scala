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

package lepus.client
package internal

import cats.effect.*
import cats.effect.implicits.*
import cats.effect.std.Mutex
import cats.effect.std.Queue
import cats.effect.std.QueueSink
import cats.effect.std.QueueSource
import cats.implicits.*
import fs2.Pipe
import fs2.Stream
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import lepus.client.Connection.Status
import lepus.client.apis.*
import lepus.protocol.*
import lepus.protocol.constants.ReplyCategory
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*

import scala.concurrent.duration.*

private[client] trait ConnectionLowLevel2[F[_]] {
  def handler: Pipe[F, Frame, Nothing]

  def newChannel: Resource[F, ChannelTransmitter[F]]

  def signal: Signal[F, Connection.Status]
  def channels: Signal[F, Set[ChannelNumber]]
}

private[client] object ConnectionLowLevel2 {
  private def openConnection[F[_]: Concurrent](
      output: QueueSink[F, Frame],
      vhost: Path
  ) = Resource.make(
    output.offer(Frame.Method(ChannelNumber(0), ConnectionClass.Open(vhost)))
  )(_ =>
    output.offer(
      Frame.Method(
        ChannelNumber(0),
        ConnectionClass.Close(
          ReplyCode.ReplySuccess,
          ShortString(""),
          ClassId(0),
          MethodId(0)
        )
      )
    )
  )

  def apply[F[_]: Temporal](
      config: NegotiatedConfig,
      vhost: Path,
      output: QueueSink[F, Frame],
      buildChannel: ChannelFactory[F]
  ): Resource[F, ConnectionLowLevel2[F]] = FrameDispatcher[F].toResource
    .flatMap(from(config, vhost, _, output, buildChannel))

  def from[F[_]: Temporal](
      config: NegotiatedConfig,
      vhost: Path,
      dispatcher: FrameDispatcher[F],
      output: QueueSink[F, Frame],
      buildChannel: ChannelFactory[F]
  ): Resource[F, ConnectionLowLevel2[F]] = for {
    state <- SignallingRef[F].of(Status.Connecting).toResource
    _ <- openConnection(output, vhost)
  } yield new {

    private val isClosed = signal.map(_ == Status.Closed)
    private val heartbeats = Stream
      .awakeEvery(config.heartbeat.toInt.seconds)
      .foreach(_ => output.offer(Frame.Heartbeat))

    override def handler: Pipe[F, Frame, Nothing] = _.foreach {
      case b: Frame.Body   => dispatcher.body(b)
      case h: Frame.Header => dispatcher.header(h)
      case Frame.Method(0, value) =>
        value match {
          case ConnectionClass.OpenOk => state.set(Status.Connected2)
          case _: ConnectionClass.Close =>
            state.set(Status.Closed) >>
              output.offer(
                Frame.Method(ChannelNumber(0), ConnectionClass.CloseOk)
              )
          case ConnectionClass.CloseOk => state.set(Status.Closed)
          case _                       => ???
        }
      case m: Frame.Method => dispatcher.invoke(m)
      case Frame.Heartbeat => output.offer(Frame.Heartbeat)
    }.onFinalize(state.set(Status.Closed))
      .concurrently(heartbeats)

    import Channel.call
    private def openChannel(ch: LowlevelChannel[F]) =
      Resource.make(ch.call(ChannelClass.Open).void)(_ =>
        // ch.status.get
        //   .map(_ == Channel.Status.Closed)
        //   .ifM(
        //     Concurrent[F].unit,
        ch.call(
          ChannelClass.Close(
            ReplyCode.ReplySuccess,
            ShortString(""),
            ClassId(0),
            MethodId(0)
          )
        ).void
      )
      // )

    private def waitTilConnected = signal.discrete
      .flatMap {
        case Status.Connecting => Stream.empty
        case Status.Connected2 => Stream.unit
        case Status.Closed =>
          Stream.raiseError(new Exception("Connection failed"))
      }
      .head
      .compile
      .drain
      .toResource

    override def newChannel: Resource[F, ChannelTransmitter[F]] = for {
      _ <- waitTilConnected
      ch <- dispatcher.add(n =>
        buildChannel(ChannelBuildInput(n, output)).toResource
      )
      _ <- openChannel(ch)
    } yield ch

    override def signal: Signal[F, Status] = state

    override def channels: Signal[F, Set[ChannelNumber]] = dispatcher.channels

  }
}

type ChannelFactory[F[_]] = ChannelBuildInput[F] => F[LowlevelChannel[F]]

final case class ChannelBuildInput[F[_]](
    number: ChannelNumber,
    output: QueueSink[F, Frame]
)
