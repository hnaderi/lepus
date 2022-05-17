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

import cats.MonadError
import cats.implicits.*
import lepus.protocol.*
import lepus.protocol.classes.*
import lepus.protocol.constants.*
import lepus.protocol.domains.*

private[client] final class ConnectionAPI[F[_]](rpc: RPCChannel[F])(using
    F: MonadError[F, Throwable]
) {

  def startOk(
      clientProperties: PeerProperties,
      mechanism: ShortString,
      response: LongString,
      locale: ShortString
  ): F[Unit] = {
    val msg =
      ConnectionClass.StartOk(clientProperties, mechanism, response, locale)
    rpc.sendNoWait(msg)
  }

  def secureOk(response: LongString): F[Unit] = {
    val msg = ConnectionClass.SecureOk(response)
    rpc.sendNoWait(msg)
  }

  def tuneOk(channelMax: Short, frameMax: Int, heartbeat: Short): F[Unit] = {
    val msg = ConnectionClass.TuneOk(channelMax, frameMax, heartbeat)
    rpc.sendNoWait(msg)
  }

  def open(virtualHost: Path): F[ConnectionClass.OpenOk.type] = {
    val msg = ConnectionClass.Open(virtualHost)
    rpc.sendWait(msg).flatMap {
      case m: ConnectionClass.OpenOk.type => m.pure
      case _                              => F.raiseError(???)
    }
  }

  def close(
      replyCode: ReplyCode,
      replyText: ReplyText,
      classId: ClassId,
      methodId: MethodId
  ): F[ConnectionClass.CloseOk.type] = {
    val msg = ConnectionClass.Close(replyCode, replyText, classId, methodId)
    rpc.sendWait(msg).flatMap {
      case m: ConnectionClass.CloseOk.type => m.pure
      case _                               => F.raiseError(???)
    }
  }

  def closeOk: F[Unit] = {
    val msg = ConnectionClass.CloseOk
    rpc.sendNoWait(msg)
  }

  def blocked(reason: ShortString): F[Unit] = {
    val msg = ConnectionClass.Blocked(reason)
    rpc.sendNoWait(msg)
  }

  def unblocked: F[Unit] = {
    val msg = ConnectionClass.Unblocked
    rpc.sendNoWait(msg)
  }

  def updateSecretOk: F[Unit] = {
    val msg = ConnectionClass.UpdateSecretOk
    rpc.sendNoWait(msg)
  }

}
private[client] final class ChannelAPI[F[_]](rpc: RPCChannel[F])(using
    F: MonadError[F, Throwable]
) {

  def open: F[ChannelClass.OpenOk.type] = {
    val msg = ChannelClass.Open
    rpc.sendWait(msg).flatMap {
      case m: ChannelClass.OpenOk.type => m.pure
      case _                           => F.raiseError(???)
    }
  }

  def flow(active: Boolean): F[ChannelClass.FlowOk] = {
    val msg = ChannelClass.Flow(active)
    rpc.sendWait(msg).flatMap {
      case m: ChannelClass.FlowOk => m.pure
      case _                      => F.raiseError(???)
    }
  }

  def flowOk(active: Boolean): F[Unit] = {
    val msg = ChannelClass.FlowOk(active)
    rpc.sendNoWait(msg)
  }

  def close(
      replyCode: ReplyCode,
      replyText: ReplyText,
      classId: ClassId,
      methodId: MethodId
  ): F[ChannelClass.CloseOk.type] = {
    val msg = ChannelClass.Close(replyCode, replyText, classId, methodId)
    rpc.sendWait(msg).flatMap {
      case m: ChannelClass.CloseOk.type => m.pure
      case _                            => F.raiseError(???)
    }
  }

  def closeOk: F[Unit] = {
    val msg = ChannelClass.CloseOk
    rpc.sendNoWait(msg)
  }

}
private[client] final class ExchangeAPI[F[_]](rpc: RPCChannel[F])(using
    F: MonadError[F, Throwable]
) {

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
private[client] final class QueueAPI[F[_]](rpc: RPCChannel[F])(using
    F: MonadError[F, Throwable]
) {

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
private[client] final class BasicAPI[F[_]](rpc: RPCChannel[F])(using
    F: MonadError[F, Throwable]
) {

  def qos(
      prefetchSize: Int,
      prefetchCount: Short,
      global: Boolean
  ): F[BasicClass.QosOk.type] = {
    val msg = BasicClass.Qos(prefetchSize, prefetchCount, global)
    rpc.sendWait(msg).flatMap {
      case m: BasicClass.QosOk.type => m.pure
      case _                        => F.raiseError(???)
    }
  }

  def consume(
      queue: QueueName,
      consumerTag: ConsumerTag,
      noLocal: NoLocal,
      noAck: NoAck,
      exclusive: Boolean,
      noWait: NoWait,
      arguments: FieldTable
  ): F[Option[BasicClass.ConsumeOk]] = {
    val msg = BasicClass.Consume(
      queue,
      consumerTag,
      noLocal,
      noAck,
      exclusive,
      noWait,
      arguments
    )
    if noWait then rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: BasicClass.ConsumeOk => m.pure
          case _                       => F.raiseError(???)
        }
        .map(_.some)
  }

  def cancel(
      consumerTag: ConsumerTag,
      noWait: NoWait
  ): F[Option[BasicClass.CancelOk]] = {
    val msg = BasicClass.Cancel(consumerTag, noWait)
    if noWait then rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: BasicClass.CancelOk => m.pure
          case _                      => F.raiseError(???)
        }
        .map(_.some)
  }

  def cancelOk(consumerTag: ConsumerTag): F[Unit] = {
    val msg = BasicClass.CancelOk(consumerTag)
    rpc.sendNoWait(msg)
  }

  def publish(
      exchange: ExchangeName,
      routingKey: ShortString,
      mandatory: Boolean,
      immediate: Boolean
  ): F[Unit] = {
    val msg = BasicClass.Publish(exchange, routingKey, mandatory, immediate)
    rpc.sendNoWait(msg)
  }

  def get(
      queue: QueueName,
      noAck: NoAck
  ): F[BasicClass.GetOk | BasicClass.GetEmpty.type] = {
    val msg = BasicClass.Get(queue, noAck)
    rpc.sendWait(msg).flatMap {
      case m: BasicClass.GetOk         => m.pure
      case m: BasicClass.GetEmpty.type => m.pure
      case _                           => F.raiseError(???)
    }
  }

  def ack(deliveryTag: DeliveryTag, multiple: Boolean): F[Unit] = {
    val msg = BasicClass.Ack(deliveryTag, multiple)
    rpc.sendNoWait(msg)
  }

  def reject(deliveryTag: DeliveryTag, requeue: Boolean): F[Unit] = {
    val msg = BasicClass.Reject(deliveryTag, requeue)
    rpc.sendNoWait(msg)
  }

  def recoverAsync(requeue: Boolean): F[Unit] = {
    val msg = BasicClass.RecoverAsync(requeue)
    rpc.sendNoWait(msg)
  }

  def recover(requeue: Boolean): F[Unit] = {
    val msg = BasicClass.Recover(requeue)
    rpc.sendNoWait(msg)
  }

  def nack(
      deliveryTag: DeliveryTag,
      multiple: Boolean,
      requeue: Boolean
  ): F[Unit] = {
    val msg = BasicClass.Nack(deliveryTag, multiple, requeue)
    rpc.sendNoWait(msg)
  }

}
private[client] final class TxAPI[F[_]](rpc: RPCChannel[F])(using
    F: MonadError[F, Throwable]
) {

  def select: F[TxClass.SelectOk.type] = {
    val msg = TxClass.Select
    rpc.sendWait(msg).flatMap {
      case m: TxClass.SelectOk.type => m.pure
      case _                        => F.raiseError(???)
    }
  }

  def commit: F[TxClass.CommitOk.type] = {
    val msg = TxClass.Commit
    rpc.sendWait(msg).flatMap {
      case m: TxClass.CommitOk.type => m.pure
      case _                        => F.raiseError(???)
    }
  }

  def rollback: F[TxClass.RollbackOk.type] = {
    val msg = TxClass.Rollback
    rpc.sendWait(msg).flatMap {
      case m: TxClass.RollbackOk.type => m.pure
      case _                          => F.raiseError(???)
    }
  }

}
private[client] final class ConfirmAPI[F[_]](rpc: RPCChannel[F])(using
    F: MonadError[F, Throwable]
) {

  def select(noWait: NoWait): F[Option[ConfirmClass.SelectOk.type]] = {
    val msg = ConfirmClass.Select(noWait)
    if noWait then rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: ConfirmClass.SelectOk.type => m.pure
          case _                             => F.raiseError(???)
        }
        .map(_.some)
  }

}
