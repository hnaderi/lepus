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

import cats.Applicative
import cats.Monad
import cats.effect.implicits.*
import cats.implicits.*
import lepus.protocol.*
import lepus.protocol.domains.ChannelNumber

sealed trait ConnectionHandler[F[_]] {
  def handle(f: Frame): F[ConnectionHandler[F]]
}

object ConnectionHandler {
  sealed abstract class Base[F[_]: Applicative] extends ConnectionHandler[F] {
    self =>
    def handle(f: Frame): F[ConnectionHandler[F]] = f match {
      case f: (Frame.Body | Frame.Header | Frame.Method) => handleOther(f)
      case Frame.Heartbeat                               => self.pure
    }

    def handleOther(
        f: Frame.Body | Frame.Header | Frame.Method
    ): F[ConnectionHandler[F]]
  }

  final case class Connected[F[_]: Monad](
      channels: Map[ChannelNumber, ChannelReceiver[F]],
      nextChannel: ChannelNumber = ChannelNumber(1)
  ) extends Base[F] {

    def handleOther(
        f: Frame.Body | Frame.Header | Frame.Method
    ): F[ConnectionHandler[F]] = copy(
      nextChannel = ChannelNumber((nextChannel + 1).toShort)
    ).pure

  }
}
