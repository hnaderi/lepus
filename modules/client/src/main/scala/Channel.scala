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

import internal.RPCCallDef
import lepus.client.apis.Messaging
import lepus.client.apis.DefaultMessaging

trait LowLevelChannel[F[_]] {
  def rpc: RPCChannel[F]
}

trait APIChannel[F[_]] {
  def exchange: ExchangeAPI[F]
  def queue: QueueAPI[F]
}

trait MessagingChannel[F[_]] extends APIChannel[F] {
  def messaging: DefaultMessaging[F]
}

trait ReliableMessagingChannel[F[_]] extends APIChannel[F] {
  def messaging: ReliableMessagingChannel[F]
}

object Channel {
  extension [M <: Method](m: M) {
    def call[F[_], O](rpc: RPCChannel[F])(using r: RPCCallDef[F, M, O]): F[O] =
      r.call(rpc)(m)
    def call[F[_], O](ch: LowLevelChannel[F])(using
        r: RPCCallDef[F, M, O]
    ): F[O] =
      r.call(ch.rpc)(m)
  }

  // val c1 :Channel[List] = ???
  // val r = BasicClass.Qos(1, 1, true).call(c1)
  // val r2 = call(BasicClass.QosOk)
}
