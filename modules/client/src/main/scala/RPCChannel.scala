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
import cats.effect.std.Queue
import cats.implicits.*
import lepus.protocol.Frame
import lepus.protocol.*
import lepus.protocol.domains.*

trait RPCChannel[F[_]] {
  def sendWait(m: Method): F[Method]
  def sendNoWait(m: Method): F[Unit]
  def recv(m: Method): F[Unit]
}

trait ContentChannel[F[_]] {
  def send(msg: Message): F[Unit]
  def recv: F[Message]
}

object ContentChannel {
  def apply[F[_]](
      channelNumber: ChannelNumber,
      maxSize: Long,
      q: Queue[F, Frame]
  )(using
      F: Concurrent[F]
  ): F[ContentChannel[F]] =
    F.unit.map(_ =>
      new {
        def send(msg: Message): F[Unit] = q.offer(
          Frame.Header(
            channelNumber,
            ClassId(10),
            msg.payload.size,
            msg.properties
          )
        ) >> q.offer(Frame.Body(channelNumber, msg.payload))
        def recv: F[Message] = ???
      }
    )
}
