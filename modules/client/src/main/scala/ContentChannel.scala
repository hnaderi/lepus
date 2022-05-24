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
import lepus.protocol.constants.ErrorCode
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*
import scodec.bits.ByteVector

import ContentChannel.*

private[client] trait ContentChannel[F[_]] {
  def send(method: BasicClass.Publish, msg: Message): F[Unit]
  def asyncNotify(m: ContentMethod): F[Unit | ErrorCode]
  def recv(h: Frame.Header | Frame.Body): F[Unit | ErrorCode]
  def consume: QueueSource[F, AsyncContent]
  def get(m: BasicClass.GetOk): F[DeferredSource[F, SynchronousGet]]

  def get_(m: BasicClass.Get): F[DeferredSource[F, Option[SynchronousGet]]] =
    ???
  def syncNotify(
      m: BasicClass.GetOk | BasicClass.GetEmpty.type
  ): F[Unit | ErrorCode] = ???
}

private[client] object ContentChannel {

  def apply[F[_]](
      channelNumber: ChannelNumber,
      maxSize: Long,
      publisher: SequentialOutput[F, Frame]
  )(using
      F: Concurrent[F]
  ): F[ContentChannel[F]] =
    assert(maxSize > 0)
    for {
      state <- F.ref[State[F]](State.Idle[F]())
      q <- Queue.bounded[F, AsyncContent](10)
    } yield new {
      private val idle = State.Idle[F]()

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

      def asyncNotify(m: ContentMethod): F[Unit | ErrorCode] =
        state.set(State.AsyncStarted(m)).widen
      def consume: QueueSource[F, AsyncContent] = q
      def recv(h: Frame.Header | Frame.Body): F[Unit | ErrorCode] =
        state.get.flatMap {
          case State.AsyncStarted(m, acc) =>
            acc
              .add(h)
              .fold(unexpected)(checkAsync(m, _).widen)
          case State.SyncStarted(d, m, acc) =>
            acc.add(h) match {
              case Some(nacc) =>
                if nacc.isCompleted then
                  d.complete(
                    SynchronousGet(
                      m.deliveryTag,
                      m.redelivered,
                      m.exchange,
                      m.routingKey,
                      m.messageCount,
                      Message(nacc.content, nacc.header.props)
                    )
                  ).as(())
                else state.set(State.SyncStarted(d, m, nacc)).widen
              case None => unexpected
            }
          case _ => unexpected
        }

      private val unexpected: F[Unit | ErrorCode] =
        ReplyCode.UnexpectedFrame.pure

      private def reset = state.set(idle)

      private def checkAsync(
          m: ContentMethod,
          nacc: Accumulator.Started
      ): F[Unit] =
        if nacc.isCompleted then q.offer(build(m, nacc))
        else state.set(State.AsyncStarted(m, nacc))

      private def build(
          m: ContentMethod,
          nacc: Accumulator.Started
      ): AsyncContent = m match {
        case m: BasicClass.Deliver =>
          DeliveredMessage(
            m.consumerTag,
            m.deliveryTag,
            m.redelivered,
            m.exchange,
            m.routingKey,
            Message(nacc.content, nacc.header.props)
          )
        case m: BasicClass.Return =>
          ReturnedMessage(
            m.replyCode,
            m.replyText,
            m.exchange,
            m.routingKey,
            Message(nacc.content, nacc.header.props)
          )
      }

      def get(m: BasicClass.GetOk): F[DeferredSource[F, SynchronousGet]] = for {
        d <- F.deferred[SynchronousGet]
        _ <- state.set(State.SyncStarted(d, m))
      } yield d
    }

  private sealed trait State[F[_]]
  private object State {
    final case class Idle[F[_]]() extends State[F]
    final case class AsyncStarted[F[_]](
        method: ContentMethod,
        acc: Accumulator = Accumulator.New
    ) extends State[F]
    final case class SyncStarted[F[_]](
        out: Deferred[F, SynchronousGet],
        method: BasicClass.GetOk,
        acc: Accumulator = Accumulator.New
    ) extends State[F]
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
