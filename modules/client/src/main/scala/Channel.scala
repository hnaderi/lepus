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

import cats.MonadError
import cats.effect.Concurrent
import cats.effect.std.Queue
import cats.implicits.*
import fs2.Stream
import lepus.client.internal.*
import lepus.protocol.*
import lepus.protocol.frame.Frame

trait Channel[F[_]] {
  def listen: Stream[F, ClientEvent]
  def exchange: ExchangeAPI[F]
  def queue: QueueAPI[F]
  def basic: BasicAPI[F]
  def tx: TxAPI[F]
  def confirm: ConfirmAPI[F]
}

object Channel {

  def apply[F[_]](
      rpc: RPCChannel[F],
      q: Queue[F, ClientEvent],
      bufferSize: Int = 100
  )(using MonadError[F, Throwable]): Channel[F] = new {
    def exchange: ExchangeAPI[F] = ExchangeAPI(rpc)
    def queue: QueueAPI[F] = QueueAPI(rpc)
    def basic: BasicAPI[F] = BasicAPI(rpc)
    def tx: TxAPI[F] = TxAPI(rpc)
    def confirm: ConfirmAPI[F] = ConfirmAPI(rpc)
    def listen: Stream[F, ClientEvent] =
      Stream.fromQueueUnterminated(q, bufferSize)
  }

}
