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
import cats.syntax.all.*
import fs2.Stream
import lepus.client.*
import lepus.client.apis.NormalMessagingChannel
import lepus.protocol.domains.*

trait WorkPoolClient[F[_], T] {
  def jobs: Stream[F, Job[T]]
  def processed(job: Job[T]): F[Unit]
}

trait WorkPoolServer[F[_], T] {
  def publish(payload: T): F[Unit]
}

final case class Job[T](
    payload: T,
    tag: DeliveryTag
)

/** WorkPoolChannel implements a work pool topology.
  *
  * In this topology, one or more peers produce jobs, and one or more workers
  * compete over processing those jobs. This topology handles workers fail over,
  * so if a worker fails, its jobs will be routed to another worker. However
  * this topology can't guarantee any ordering of messages by definition.
  */
object WorkPoolChannel {

  /** Publisher peer in a [[lepus.std.WorkPoolChannel]] topology */
  def publisher[F[_]: Concurrent, T](
      pool: WorkPoolDefinition[T],
      ch: Channel[F, NormalMessagingChannel[F]]
  ): F[WorkPoolServer[F, T]] = for {
    _ <- ch.queue.declare(pool.name, durable = true)
  } yield new {
    override def publish(payload: T): F[Unit] = pool.codec
      .encode(payload)
      .fold(
        _.raiseError,
        msg =>
          ch.messaging
            .publishRaw(ExchangeName.default, routingKey = pool.name, msg)
      )
  }

  /** Worker peer in a [[lepus.std.WorkPoolChannel]] topology */
  def worker[F[_]: Concurrent, T](
      pool: WorkPoolDefinition[T],
      ch: Channel[F, NormalMessagingChannel[F]]
  ): F[WorkPoolClient[F, T]] = for {
    _ <- ch.queue.declare(pool.name, durable = true)
  } yield new {

    override def jobs: Stream[F, Job[T]] = ch.messaging
      .consumeRaw(pool.name, noAck = false)
      .flatMap(env =>
        pool.codec
          .decode(env.message)
          .fold(
            _ => Stream.exec(ch.messaging.reject(env.deliveryTag, false)),
            msg => Stream.emit(Job(msg.payload, env.deliveryTag))
          )
      )

    override def processed(job: Job[T]): F[Unit] = ch.messaging.ack(job.tag)

  }
}
