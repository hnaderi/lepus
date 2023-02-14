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
import cats.effect.kernel.Deferred
import cats.effect.kernel.DeferredSource
import cats.effect.std.Queue
import cats.effect.std.QueueSink
import cats.effect.std.QueueSource
import cats.implicits.*
import fs2.Stream
import lepus.protocol.ConnectionClass.Start
import lepus.protocol.Frame
import lepus.protocol.*
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*
import scodec.bits.ByteVector

import ContentChannel.*

private[client] trait ContentChannel[F[_]] {
  def asyncNotify(m: ContentMethod): F[Unit]
  def syncNotify(m: ContentSyncResponse): F[Unit]

  def recv(h: Frame.Header | Frame.Body): F[Unit]
  def abort: F[Unit]

  def get(m: BasicClass.Get): F[DeferredSource[F, Option[SynchronousGetRaw]]]
}

private[client] object ContentChannel {

  def apply[F[_]](
      channelNumber: ChannelNumber,
      publisher: SequentialOutput[F, Frame],
      dispatcher: MessageDispatcher[F],
      getList: Waitlist[F, Option[SynchronousGetRaw]]
  )(using
      F: Concurrent[F]
  ): F[ContentChannel[F]] =
    for {
      state <- F.ref(State.Idle)
    } yield new {

      private val unexpected: F[Unit] =
        F.raiseError(
          AMQPError(
            ReplyCode.UnexpectedFrame,
            replyText = ShortString(
              "Received an unexpected frame, this is a fatal protocol error"
            ),
            ClassId(0),
            MethodId(0)
          )
        )

      def asyncNotify(m: ContentMethod): F[Unit] =
        state.set(State.AsyncStarted(m))

      def recv(h: Frame.Header | Frame.Body): F[Unit] =
        state.get.flatMap {
          case State.AsyncStarted(m, acc) =>
            acc
              .add(h)
              .fold(unexpected)(checkAsync(m, _))
          case State.SyncStarted(m, acc) =>
            acc.add(h).fold(unexpected)(checkSync(m, _))
          case _ => unexpected
        }

      def abort: F[Unit] = reset

      private def reset = state.set(State.Idle)

      private def checkAsync(
          m: ContentMethod,
          nacc: Accumulator.Started
      ) =
        if nacc.isCompleted then
          build(m, nacc) match {
            case d: DeliveredMessageRaw => dispatcher.deliver(d)
            case r: ReturnedMessageRaw  => dispatcher.`return`(r)
          }
        else state.set(State.AsyncStarted(m, nacc))

      private def checkSync(
          m: BasicClass.GetOk,
          nacc: Accumulator.Started
      ): F[Unit] =
        if nacc.isCompleted then
          respond(
            SynchronousGetRaw(
              m.deliveryTag,
              m.redelivered,
              m.exchange,
              m.routingKey,
              m.messageCount,
              MessageRaw(nacc.content, nacc.header.props)
            ).some
          )
        else state.set(State.SyncStarted(m, nacc)).widen

      private def build(
          m: ContentMethod,
          nacc: Accumulator.Started
      ): AsyncContent = m match {
        case m: BasicClass.Deliver =>
          DeliveredMessageRaw(
            m.consumerTag,
            m.deliveryTag,
            m.redelivered,
            m.exchange,
            m.routingKey,
            MessageRaw(nacc.content, nacc.header.props)
          )
        case m: BasicClass.Return =>
          ReturnedMessageRaw(
            m.replyCode,
            m.replyText,
            m.exchange,
            m.routingKey,
            MessageRaw(nacc.content, nacc.header.props)
          )
      }

      def get(
          m: BasicClass.Get
      ): F[DeferredSource[F, Option[SynchronousGetRaw]]] =
        getList.checkinAnd(publisher.writeOne(Frame.Method(channelNumber, m)))

      private def respond(o: Option[SynchronousGetRaw]): F[Unit] =
        getList.nextTurn(o).map(if _ then () else ReplyCode.SyntaxError)

      def syncNotify(m: ContentSyncResponse): F[Unit] = m match {
        case m: BasicClass.GetOk => state.set(State.SyncStarted(m)).widen
        case BasicClass.GetEmpty => respond(None)
      }
    }

  private enum State {
    case Idle
    case AsyncStarted(
        method: ContentMethod,
        acc: Accumulator = Accumulator.New
    )
    case SyncStarted(
        method: BasicClass.GetOk,
        acc: Accumulator = Accumulator.New
    )
  }

  private enum Accumulator {
    case New
    case Started(header: Frame.Header, content: ByteVector)

    def addHeader(f: Frame.Header): Option[Started] = this match {
      case New => Some(Started(f, ByteVector.empty))
      case _   => None
    }

    def addBody(f: Frame.Body): Option[Started] = this match {
      case Started(h, c) if h.channel == f.channel =>
        Some(Started(h, c ++ f.payload))
      case _ => None
    }

    def add(f: Frame.Header | Frame.Body): Option[Started] = f match {
      case f: Frame.Header => addHeader(f)
      case f: Frame.Body   => addBody(f)
    }

    def isCompleted: Boolean = this match {
      case Started(h, c) if h.bodySize == c.size => true
      case _                                     => false
    }
  }
}
