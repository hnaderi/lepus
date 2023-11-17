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
import lepus.protocol.*
import lepus.protocol.domains.*

import internal.ChannelTransmitter
import Channel.call

trait ExchangeAPI[F[_]] {

  def declare(
      exchange: ExchangeName,
      `type`: ExchangeType,
      passive: Boolean = false,
      durable: Boolean = false,
      autoDelete: Boolean = true,
      internal: Boolean = false,
      noWait: NoWait = false,
      arguments: FieldTable = FieldTable.empty
  ): F[Option[ExchangeClass.DeclareOk.type]]

  def delete(
      exchange: ExchangeName,
      ifUnused: Boolean = true,
      noWait: NoWait = false
  ): F[Option[ExchangeClass.DeleteOk.type]]

  def bind(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait = false,
      arguments: FieldTable = FieldTable.empty
  ): F[Option[ExchangeClass.BindOk.type]]

  def unbind(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait = false,
      arguments: FieldTable = FieldTable.empty
  ): F[Option[ExchangeClass.UnbindOk.type]]

}
trait QueueAPI[F[_]] {

  def declare(
      queue: QueueName = QueueName(""),
      passive: Boolean = false,
      durable: Boolean = false,
      exclusive: Boolean = false,
      autoDelete: Boolean = false,
      noWait: NoWait = false,
      arguments: FieldTable = FieldTable.empty
  ): F[Option[QueueClass.DeclareOk]]

  def bind(
      queue: QueueName,
      exchange: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait = false,
      arguments: FieldTable = FieldTable.empty
  ): F[Option[QueueClass.BindOk.type]]

  def unbind(
      queue: QueueName,
      exchange: ExchangeName,
      routingKey: ShortString,
      arguments: FieldTable = FieldTable.empty
  ): F[QueueClass.UnbindOk.type]

  def purge(
      queue: QueueName,
      noWait: NoWait = false
  ): F[Option[QueueClass.PurgeOk]]

  def delete(
      queue: QueueName,
      ifUnused: Boolean = true,
      ifEmpty: Boolean = true,
      noWait: NoWait = false
  ): F[Option[QueueClass.DeleteOk]]
}

private[client] final class ExchangeAPIImpl[F[_]](rpc: ChannelTransmitter[F])(
    using F: MonadError[F, Throwable]
) extends ExchangeAPI[F] {

  def declare(
      exchange: ExchangeName,
      `type`: ExchangeType,
      passive: Boolean,
      durable: Boolean,
      autoDelete: Boolean,
      internal: Boolean,
      noWait: NoWait,
      arguments: FieldTable
  ): F[Option[ExchangeClass.DeclareOk.type]] =
    rpc.call(
      ExchangeClass.Declare(
        exchange,
        `type`,
        passive,
        durable,
        autoDelete,
        internal,
        noWait,
        arguments
      )
    )

  def delete(
      exchange: ExchangeName,
      ifUnused: Boolean,
      noWait: NoWait
  ): F[Option[ExchangeClass.DeleteOk.type]] =
    rpc.call(ExchangeClass.Delete(exchange, ifUnused, noWait))

  def bind(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait,
      arguments: FieldTable
  ): F[Option[ExchangeClass.BindOk.type]] =
    rpc.call(
      ExchangeClass.Bind(destination, source, routingKey, noWait, arguments)
    )

  def unbind(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait,
      arguments: FieldTable
  ): F[Option[ExchangeClass.UnbindOk.type]] =
    rpc.call(
      ExchangeClass.Unbind(destination, source, routingKey, noWait, arguments)
    )
}

private[client] final class QueueAPIImpl[F[_]](rpc: ChannelTransmitter[F])(using
    F: MonadError[F, Throwable]
) extends QueueAPI[F] {

  def declare(
      queue: QueueName,
      passive: Boolean,
      durable: Boolean,
      exclusive: Boolean,
      autoDelete: Boolean,
      noWait: NoWait,
      arguments: FieldTable
  ): F[Option[QueueClass.DeclareOk]] =
    rpc.call(
      QueueClass.Declare(
        queue,
        passive,
        durable,
        exclusive,
        autoDelete,
        noWait,
        arguments
      )
    )

  def bind(
      queue: QueueName,
      exchange: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait,
      arguments: FieldTable
  ): F[Option[QueueClass.BindOk.type]] =
    rpc.call(QueueClass.Bind(queue, exchange, routingKey, noWait, arguments))

  def unbind(
      queue: QueueName,
      exchange: ExchangeName,
      routingKey: ShortString,
      arguments: FieldTable
  ): F[QueueClass.UnbindOk.type] =
    rpc.call(QueueClass.Unbind(queue, exchange, routingKey, arguments))

  def purge(queue: QueueName, noWait: NoWait): F[Option[QueueClass.PurgeOk]] =
    rpc.call(QueueClass.Purge(queue, noWait))

  def delete(
      queue: QueueName,
      ifUnused: Boolean,
      ifEmpty: Boolean,
      noWait: NoWait
  ): F[Option[QueueClass.DeleteOk]] =
    rpc.call(QueueClass.Delete(queue, ifUnused, ifEmpty, noWait))

}
