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
import cats.effect.kernel.Resource
import cats.effect.std.*
import cats.implicits.*
import fs2.Stream
import lepus.protocol.domains.ConsumerTag

private[client] trait Dispatcher[F[_]] {
  def deliver(msg: DeliveredMessage): F[Unit]
  def `return`(msg: ReturnedMessage): F[Unit]
  def deliveryQ(
      ctag: ConsumerTag
  ): Resource[F, QueueSource[F, DeliveredMessage]]
  def returnQ: QueueSource[F, ReturnedMessage]
}
