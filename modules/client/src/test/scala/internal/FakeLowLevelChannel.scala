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
import cats.effect.std.*
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.Stream
import fs2.concurrent.Signal
import lepus.client.Channel.Status
import lepus.client.DeliveredMessage
import lepus.client.Message
import lepus.client.ReturnedMessage
import lepus.client.SynchronousGet
import lepus.client.internal.ConnectionLowLevel
import lepus.client.internal.FakeLowLevelChannel.Interaction
import lepus.codecs.ConnectionDataGenerator
import lepus.codecs.DomainGenerators
import lepus.codecs.FrameGenerators
import lepus.protocol.BasicClass.Get
import lepus.protocol.BasicClass.Publish
import lepus.protocol.*
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*
import fs2.concurrent.SignallingRef

final class FakeLowLevelChannel(
    val interactions: InteractionList[Interaction],
    channel: Deferred[IO, LowlevelChannel[IO]]
) extends LowlevelChannel[IO] {
  private def call[T](f: LowlevelChannel[IO] => IO[T]) = channel.get.flatMap(f)
  private val channelS = Stream.eval(channel.get)

  override def asyncContent(m: ContentMethod): IO[Unit] = call(
    _.asyncContent(m)
  )

  override def status: Signal[cats.effect.IO, Status] = new {

    override def discrete: Stream[cats.effect.IO, Status] =
      channelS.flatMap(_.status.discrete)

    override def continuous: Stream[cats.effect.IO, Status] =
      channelS.flatMap(_.status.continuous)

    override def get: IO[Status] = call(_.status.get)

  }

  override def delivered(
      ctag: ConsumerTag
  ): Stream[cats.effect.IO, DeliveredMessage] =
    channelS.flatMap(_.delivered(ctag))

  override def header(h: Frame.Header): IO[Unit] = call(_.header(h))

  override def body(h: Frame.Body): IO[Unit] = call(_.body(h))

  override def get(m: Get): IO[Option[SynchronousGet]] = call(_.get(m))

  override def method(m: Method): IO[Unit] =
    call(_.method(m))

  override def publish(method: Publish, msg: Message): IO[Unit] =
    call(_.publish(method, msg))

  override def syncContent(m: ContentSyncResponse): IO[Unit] =
    call(_.syncContent(m))

  override def returned: Stream[cats.effect.IO, ReturnedMessage] =
    Stream.eval(channel.get).flatMap(_.returned)

  override def sendWait(m: Method): IO[Method] =
    interactions.add(Interaction.SendWait(m)) >> call(_.sendWait(m))

  override def sendNoWait(m: Method): IO[Unit] =
    interactions.add(Interaction.SendNoWait(m)) >> call(_.sendNoWait(m))

  def setChannel(ch: LowlevelChannel[IO]): IO[FakeLowLevelChannel] = channel
    .complete(ch)
    .ifM(IO(this), IO.raiseError(new Exception("Cannot set twice!")))
}

object FakeLowLevelChannel {
  enum Interaction {
    case SendWait(m: Method)
    case SendNoWait(m: Method)
  }
  def apply(): IO[FakeLowLevelChannel] = for {
    is <- InteractionList[Interaction]
    ch <- IO.deferred[LowlevelChannel[IO]]
  } yield new FakeLowLevelChannel(is, ch)
}
