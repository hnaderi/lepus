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
import cats.syntax.all.*
import fs2.Stream
import fs2.concurrent.Signal
import lepus.client.Channel.Status
import lepus.client.internal.FakeLowLevelChannel.Interaction
import lepus.protocol.BasicClass.Get
import lepus.protocol.BasicClass.Publish
import lepus.protocol.*
import lepus.protocol.domains.*

final class FakeLowLevelChannel(
    val interactions: InteractionList[Interaction],
    channel: Deferred[IO, LowlevelChannel[IO]]
) extends LowlevelChannel[IO] {

  private def call[T](f: LowlevelChannel[IO] => IO[T]) = channel.get.flatMap(f)
  private val channelS = Stream.eval(channel.get)

  override def onClose: IO[Unit] = call(_.onClose)

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

  override def delivered(ctag: ConsumerTag)
      : Resource[IO, (ConsumerTag, Stream[IO, DeliveredMessageRaw])] =
    Resource.eval(channel.get).flatMap(_.delivered(ctag))

  override def header(h: Frame.Header): IO[Unit] = call(_.header(h))

  override def body(h: Frame.Body): IO[Unit] = call(_.body(h))

  override def get(m: Get): IO[Option[SynchronousGetRaw]] = call(_.get(m))

  override def method(m: Method): IO[Unit] =
    call(_.method(m))

  override def publish(method: Publish, msg: MessageRaw): IO[Unit] =
    call(_.publish(method, msg))

  override def syncContent(m: ContentSyncResponse): IO[Unit] =
    call(_.syncContent(m))

  override def returned: Stream[cats.effect.IO, ReturnedMessageRaw] =
    Stream.eval(channel.get).flatMap(_.returned)

  override def sendWait(m: Method): IO[Method] =
    interactions.add(Interaction.SendWait(m)) >> call(_.sendWait(m))

  override def sendNoWait(m: Method): IO[Unit] =
    interactions.add(Interaction.SendNoWait(m)) >> call(_.sendNoWait(m))

  override def confirmed: Stream[IO, Confirmation] =
    channelS.flatMap(_.confirmed)

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
