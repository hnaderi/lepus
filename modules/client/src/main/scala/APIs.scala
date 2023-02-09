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
import cats.implicits.*
import lepus.protocol.*
import lepus.protocol.classes.*
import lepus.protocol.constants.*
import lepus.protocol.domains.*

import internal.*

trait ExchangeAPI[F[_]] {

  def declare(
      exchange: ExchangeName,
      `type`: ShortString,
      passive: Boolean,
      durable: Boolean,
      autoDelete: Boolean,
      internal: Boolean,
      noWait: NoWait,
      arguments: FieldTable
  ): F[Option[ExchangeClass.DeclareOk.type]]

  def delete(
      exchange: ExchangeName,
      ifUnused: Boolean,
      noWait: NoWait
  ): F[Option[ExchangeClass.DeleteOk.type]]

  def bind(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait,
      arguments: FieldTable
  ): F[Option[ExchangeClass.BindOk.type]]

  def unbind(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait,
      arguments: FieldTable
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
      noWait: NoWait,
      arguments: FieldTable
  ): F[Option[QueueClass.BindOk.type]]

  def unbind(
      queue: QueueName,
      exchange: ExchangeName,
      routingKey: ShortString,
      arguments: FieldTable
  ): F[QueueClass.UnbindOk.type]

  def purge(queue: QueueName, noWait: NoWait): F[Option[QueueClass.PurgeOk]]

  def delete(
      queue: QueueName,
      ifUnused: Boolean,
      ifEmpty: Boolean,
      noWait: NoWait
  ): F[Option[QueueClass.DeleteOk]]
}

private[client] final class ExchangeAPIImpl[F[_]](rpc: ChannelTransmitter[F])(
    using F: MonadError[F, Throwable]
) extends ExchangeAPI[F] {

  def declare(
      exchange: ExchangeName,
      `type`: ShortString,
      passive: Boolean,
      durable: Boolean,
      autoDelete: Boolean,
      internal: Boolean,
      noWait: NoWait,
      arguments: FieldTable
  ): F[Option[ExchangeClass.DeclareOk.type]] = {
    val msg = ExchangeClass.Declare(
      exchange,
      `type`,
      passive,
      durable,
      autoDelete,
      internal,
      noWait,
      arguments
    )
    if noWait then rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: ExchangeClass.DeclareOk.type => m.pure
          case _                               => F.raiseError(???)
        }
        .map(_.some)
  }

  def delete(
      exchange: ExchangeName,
      ifUnused: Boolean,
      noWait: NoWait
  ): F[Option[ExchangeClass.DeleteOk.type]] = {
    val msg = ExchangeClass.Delete(exchange, ifUnused, noWait)
    if noWait then rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: ExchangeClass.DeleteOk.type => m.pure
          case _                              => F.raiseError(???)
        }
        .map(_.some)
  }

  def bind(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait,
      arguments: FieldTable
  ): F[Option[ExchangeClass.BindOk.type]] = {
    val msg =
      ExchangeClass.Bind(destination, source, routingKey, noWait, arguments)
    if noWait then rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: ExchangeClass.BindOk.type => m.pure
          case _                            => F.raiseError(???)
        }
        .map(_.some)
  }

  def unbind(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait,
      arguments: FieldTable
  ): F[Option[ExchangeClass.UnbindOk.type]] = {
    val msg =
      ExchangeClass.Unbind(destination, source, routingKey, noWait, arguments)
    if noWait then rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: ExchangeClass.UnbindOk.type => m.pure
          case _                              => F.raiseError(???)
        }
        .map(_.some)
  }

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
  ): F[Option[QueueClass.DeclareOk]] = {
    val msg = QueueClass.Declare(
      queue,
      passive,
      durable,
      exclusive,
      autoDelete,
      noWait,
      arguments
    )
    if noWait then rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: QueueClass.DeclareOk => m.pure
          case _                       => F.raiseError(???)
        }
        .map(_.some)
  }

  def bind(
      queue: QueueName,
      exchange: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait,
      arguments: FieldTable
  ): F[Option[QueueClass.BindOk.type]] = {
    val msg = QueueClass.Bind(queue, exchange, routingKey, noWait, arguments)
    if noWait then rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: QueueClass.BindOk.type => m.pure
          case _                         => F.raiseError(???)
        }
        .map(_.some)
  }

  def unbind(
      queue: QueueName,
      exchange: ExchangeName,
      routingKey: ShortString,
      arguments: FieldTable
  ): F[QueueClass.UnbindOk.type] = {
    val msg = QueueClass.Unbind(queue, exchange, routingKey, arguments)
    rpc.sendWait(msg).flatMap {
      case m: QueueClass.UnbindOk.type => m.pure
      case _                           => F.raiseError(???)
    }
  }

  def purge(queue: QueueName, noWait: NoWait): F[Option[QueueClass.PurgeOk]] = {
    val msg = QueueClass.Purge(queue, noWait)
    if noWait then rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: QueueClass.PurgeOk => m.pure
          case _                     => F.raiseError(???)
        }
        .map(_.some)
  }

  def delete(
      queue: QueueName,
      ifUnused: Boolean,
      ifEmpty: Boolean,
      noWait: NoWait
  ): F[Option[QueueClass.DeleteOk]] = {
    val msg = QueueClass.Delete(queue, ifUnused, ifEmpty, noWait)
    if noWait then rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: QueueClass.DeleteOk => m.pure
          case _                      => F.raiseError(???)
        }
        .map(_.some)
  }

}
