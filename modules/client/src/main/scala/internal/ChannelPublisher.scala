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
import cats.implicits.*
import fs2.Stream
import lepus.protocol.Frame
import lepus.protocol.*
import lepus.protocol.domains.*
import scodec.bits.ByteVector

trait ChannelPublisher[F[_]] {
  def send(method: BasicClass.Publish, msg: Message): F[Unit]
}

object ChannelPublisher {
  def apply[F[_]: Concurrent](
      channelNumber: ChannelNumber,
      maxSize: Long,
      publisher: SequentialOutput[F, Frame]
  ): ChannelPublisher[F] = new {
    def send(method: BasicClass.Publish, msg: Message): F[Unit] =
      publisher.writeAll(
        List
          .range(0L, msg.payload.size, maxSize)
          .map(i =>
            Frame.Body(channelNumber, msg.payload.slice(i, i + maxSize))
          )
          .prepended(
            Frame.Header(
              channelNumber,
              ClassId(10),
              msg.payload.size,
              msg.properties
            )
          )
          .prepended(Frame.Method(channelNumber, method)): _*
      )
  }

}
