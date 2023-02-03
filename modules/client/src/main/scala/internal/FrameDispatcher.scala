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
import cats.effect.Concurrent
import cats.effect.syntax.all.*
import cats.effect.kernel.Resource
import cats.implicits.*
import lepus.protocol.*
import lepus.protocol.constants.ErrorCode
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.ChannelNumber
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef

sealed trait FrameDispatcher[F[_]] {
  def header(h: Frame.Header): F[Unit | ErrorCode]
  def body(b: Frame.Body): F[Unit | ErrorCode]
  def invoke(m: Frame.Method): F[Unit | ErrorCode]

  def add[CHANNEL <: ChannelReceiver[F]](
      build: ChannelNumber => Resource[F, CHANNEL]
  ): Resource[F, CHANNEL]
  def channels: Signal[F, Set[ChannelNumber]]
}

object FrameDispatcher {
  private final case class State[F[_]](
      channels: Map[ChannelNumber, ChannelReceiver[F]] =
        Map.empty[ChannelNumber, ChannelReceiver[F]],
      nextChannel: ChannelNumber = ChannelNumber(1)
  )

  def apply[F[_]](using F: Concurrent[F]): F[FrameDispatcher[F]] = for {
    state <- SignallingRef[F].of(State[F]())
  } yield new {
    def header(h: Frame.Header): F[Unit | ErrorCode] =
      call(h.channel)(_.header(h))

    def body(b: Frame.Body): F[Unit | ErrorCode] =
      call(b.channel)(_.body(b))

    def invoke(m: Frame.Method): F[Unit | ErrorCode] = call(m.channel)(ch =>
      m.value match {
        case d: (BasicClass.Deliver | BasicClass.Return) => ch.asyncContent(d)
        case d: (BasicClass.GetOk | BasicClass.GetEmpty.type) =>
          ch.syncContent(d)
        case other => ch.method(other)
      }
    )

    private def addRecvr(
        chNum: ChannelNumber,
        recvr: ChannelReceiver[F]
    ): Resource[F, Unit] =
      Resource.make(
        state.update(s => s.copy(channels = s.channels.updated(chNum, recvr)))
      )(_ => state.update(s => s.copy(channels = s.channels.removed(chNum))))

    private def call(
        ch: ChannelNumber
    )(f: ChannelReceiver[F] => F[Unit | ErrorCode]): F[Unit | ErrorCode] =
      state.get.map(_.channels.get(ch)).flatMap {
        case Some(r) => f(r)
        case None    => ReplyCode.ChannelError.pure
      }

    private def getNextChannelNumber = state.modify { s =>
      val nc = ChannelNumber((s.nextChannel + 1).toShort)
      val ns = s.copy(nextChannel = nc)

      (ns, s.nextChannel)
    }

    def channels: Signal[F, Set[ChannelNumber]] = state.map(_.channels.keySet)
    def add[CHANNEL <: ChannelReceiver[F]](
        build: ChannelNumber => Resource[F, CHANNEL]
    ): Resource[F, CHANNEL] = for {
      ch <- getNextChannelNumber.toResource
      out <- build(ch)
      _ <- addRecvr(ch, out)
    } yield out

  }
}
