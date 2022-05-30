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

import cats.effect.Concurrent
import cats.effect.std.*
import cats.implicits.*
import fs2.Stream
import lepus.protocol.*
import lepus.protocol.constants.ErrorCode
import lepus.protocol.domains.ConsumerTag

type ContentMethod = BasicClass.Deliver | BasicClass.Return
type ContentSyncResponse = BasicClass.GetOk | BasicClass.GetEmpty.type

private[client] trait ChannelReceiver[F[_]] {
  def asyncContent(m: ContentMethod): F[Unit | ErrorCode]
  def syncContent(m: ContentSyncResponse): F[Unit | ErrorCode]
  def header(h: Frame.Header): F[Unit | ErrorCode]
  def body(h: Frame.Body): F[Unit | ErrorCode]
  def method(m: Method): F[Unit | ErrorCode]
  def flow(enable: Boolean): F[Unit]
  def cancel: F[Unit]
}

private[client] trait ChannelTransmitter[F[_]] {
  def publish(method: BasicClass.Publish, msg: Message): F[Unit]
  def sendWait(m: Method): F[Method]
  def sendNoWait(m: Method): F[Unit]
  def get(m: BasicClass.Get): F[Option[SynchronousGet]]
  def delivered(ctag: ConsumerTag): Stream[F, DeliveredMessage]
  def returned: Stream[F, ReturnedMessage]
}

private[client] trait LowlevelChannel[F[_]]
    extends ChannelReceiver[F],
      ChannelTransmitter[F]

private[client] object LowlevelChannel {
  sealed trait State[F[_]]
  object State {
    final case class Idle[F[_]]() extends State[F]
    final case class ReceivingAsync[F[_]]() extends State[F]
    final case class ReceivingSync[F[_]]() extends State[F]
  }

}
