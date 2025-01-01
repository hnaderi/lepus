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

package lepus.client.internal

import cats.effect.*
import cats.effect.implicits.*
import cats.implicits.*
import lepus.protocol.*
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*

private[client] type ChannelBuilder[F[_]] = Resource[F, ChannelTransmitter[F]]

private[client] object ChannelBuilder {
  def apply[F[_]: Concurrent](
      send: OutputWriterSink[F, Frame],
      status: ConnectionState[F],
      dispatcher: FrameDispatcher[F],
      buildChannel: ChannelFactory[F]
  ): ChannelBuilder[F] = for {
    _ <- status.awaitOpened.toResource
    config <- status.config.toResource
    ch <- dispatcher.add(n =>
      buildChannel(ChannelBuildInput(n, send, config.frameMax)).toResource
    )
    _ <- openChannel(ch)
  } yield ch

  import lepus.client.Channel.call
  private def openChannel[F[_]: Concurrent](ch: LowlevelChannel[F]) =
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
}

private[client] type ChannelFactory[F[_]] =
  ChannelBuildInput[F] => F[LowlevelChannel[F]]

private[client] final case class ChannelBuildInput[F[_]](
    number: ChannelNumber,
    output: OutputWriterSink[F, Frame],
    frameMax: Int
)
