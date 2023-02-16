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

package lepus.std

import cats.effect.Concurrent
import cats.effect.kernel.Clock
import cats.syntax.all.*
import fs2.Stream
import lepus.client.*
import lepus.client.apis.NormalMessagingChannel
import lepus.protocol.domains.*
import lepus.std.TopicDefinition

import java.time.Instant

trait EventPublisher[F[_], T] {
  def publish(id: ShortString, t: T): F[Unit]
}

trait EventConsumer[F[_], T] {
  def events: Stream[F, EventMessage[T]]
  def processed(evt: EventMessage[T]): F[Unit]
  def reject(evt: EventMessage[T]): F[Unit]
}

final case class EventMessage[T](
    id: ShortString,
    time: Instant,
    payload: T,
    tag: DeliveryTag
)

object EventChannel {
  def publisher[F[_]: Concurrent: Clock, T](
      topic: TopicDefinition[T],
      ch: Channel[F, NormalMessagingChannel[F]]
  ): F[EventPublisher[F, T]] = for {
    _ <- ch.exchange.declare(topic.exchange, ExchangeType.Topic)
  } yield new {
    private val currentTime = Clock[F].realTime.map(d => Timestamp(d.toMillis))

    override def publish(id: ShortString, t: T): F[Unit] =
      topic.codec
        .encode(t)
        .product(topic.topic.get(t).leftMap(InvalidTopicName(_))) match {
        case Left(error) => error.raiseError
        case Right((msg, topicName)) =>
          currentTime.flatMap(now =>
            ch.messaging.publishRaw(
              topic.exchange,
              topicName,
              msg.withMessageId(id).withTimestamp(now)
            )
          )
      }
  }

  def consumer[F[_], T](
      topic: TopicDefinition[T],
      queue: Option[QueueName] = None,
      topics: TopicSelector*
  )(
      ch: Channel[F, NormalMessagingChannel[F]]
  )(using F: Concurrent[F]): F[EventConsumer[F, T]] = for {
    _ <- ch.exchange.declare(topic.exchange, ExchangeType.Topic)
    q <- queue match {
      case None =>
        ch.queue
          .declare(QueueName.autoGen, exclusive = true)
          .flatMap(
            F.fromOption(_, new UnknownError("Must respond with Queue name"))
          )
          .map(_.queue)
      case Some(value) => ch.queue.declare(value, durable = true).as(value)
    }
    _ <- topics.toList.traverse(t =>
      ch.queue.bind(q, topic.exchange, routingKey = t)
    )
  } yield new {

    override def events: Stream[F, EventMessage[T]] = ch.messaging
      .consumeRaw(q, noAck = false)
      .flatMap(env =>
        topic.codec.decode(env.message) match {
          case Left(error) => Stream.exec(reject(env.deliveryTag))
          case Right(value) =>
            val evt = for {
              id <- value.properties.messageId
              time <- value.properties.timestamp
            } yield EventMessage(
              id,
              time.toInstant,
              value.payload,
              env.deliveryTag
            )

            evt.fold(Stream.exec(reject(env.deliveryTag)))(Stream.emit(_))
        }
      )

    override def processed(evt: EventMessage[T]): F[Unit] =
      ch.messaging.ack(evt.tag)

    override def reject(evt: EventMessage[T]): F[Unit] = reject(evt.tag)

    private def reject(dtag: DeliveryTag): F[Unit] =
      ch.messaging.reject(dtag, false)

  }

  final case class InvalidTopicName(msg: String) extends RuntimeException(msg)
}
