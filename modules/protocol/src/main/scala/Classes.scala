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

package lepus.protocol

import lepus.protocol.*
import lepus.protocol.constants.*
import lepus.protocol.domains.*

sealed trait Method {
  val _classId: ClassId
  val _methodId: MethodId
  val _synchronous: Boolean
  val _isRequest: Boolean
  val _isResponse: Boolean
}

object Metadata {
  sealed trait Async extends Method {
    override val _synchronous = false
  }
  sealed trait Sync extends Method {
    override val _synchronous = true
  }
  sealed trait Request extends Method {
    override val _isRequest = true
  }
  sealed trait Response extends Method {
    override val _isResponse = true
  }
  sealed trait NotRequest extends Method {
    override val _isRequest = false
  }
  sealed trait NotResponse extends Method {
    override val _isResponse = false
  }
}

import Metadata.*

sealed trait ConnectionClass extends Method {
  override val _classId = ClassId(10)
}

object ConnectionClass {

  final case class Start(
      versionMajor: Byte,
      versionMinor: Byte,
      serverProperties: PeerProperties,
      mechanisms: LongString,
      locales: LongString
  ) extends ConnectionClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(10)
  }

  final case class StartOk(
      clientProperties: PeerProperties,
      mechanism: ShortString,
      response: LongString,
      locale: ShortString
  ) extends ConnectionClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(11)
  }

  final case class Secure(challenge: LongString)
      extends ConnectionClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(20)
  }

  final case class SecureOk(response: LongString)
      extends ConnectionClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(21)
  }

  final case class Tune(channelMax: Short, frameMax: Int, heartbeat: Short)
      extends ConnectionClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(30)
  }

  final case class TuneOk(channelMax: Short, frameMax: Int, heartbeat: Short)
      extends ConnectionClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(31)
  }

  final case class Open(virtualHost: Path)
      extends ConnectionClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(40)
  }

  case object OpenOk
      extends ConnectionClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(41)
  }

  final case class Close(
      replyCode: ReplyCode,
      replyText: ReplyText,
      classId: ClassId,
      methodId: MethodId
  ) extends ConnectionClass
      with Sync
      with Request
      with Response {
    override val _methodId = MethodId(50)
  }

  case object CloseOk
      extends ConnectionClass
      with Sync
      with Request
      with Response {
    override val _methodId = MethodId(51)
  }

  final case class Blocked(reason: ShortString)
      extends ConnectionClass
      with Async
      with Request
      with Response {
    override val _methodId = MethodId(60)
  }

  case object Unblocked
      extends ConnectionClass
      with Async
      with Request
      with Response {
    override val _methodId = MethodId(61)
  }

  final case class UpdateSecret(newSecret: LongString, reason: ShortString)
      extends ConnectionClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(70)
  }

  case object UpdateSecretOk
      extends ConnectionClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(71)
  }
}

sealed trait ChannelClass extends Method {
  override val _classId = ClassId(20)
}

object ChannelClass {

  case object Open
      extends ChannelClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(10)
  }

  case object OpenOk
      extends ChannelClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(11)
  }

  final case class Flow(active: Boolean)
      extends ChannelClass
      with Sync
      with Request
      with Response {
    override val _methodId = MethodId(20)
  }

  final case class FlowOk(active: Boolean)
      extends ChannelClass
      with Async
      with Request
      with Response {
    override val _methodId = MethodId(21)
  }

  final case class Close(
      replyCode: ReplyCode,
      replyText: ReplyText,
      classId: ClassId,
      methodId: MethodId
  ) extends ChannelClass
      with Sync
      with Request
      with Response {
    override val _methodId = MethodId(40)
  }

  case object CloseOk
      extends ChannelClass
      with Sync
      with Request
      with Response {
    override val _methodId = MethodId(41)
  }
}

sealed trait ExchangeClass extends Method {
  override val _classId = ClassId(40)
}

object ExchangeClass {

  final case class Declare(
      exchange: ExchangeName,
      `type`: ShortString,
      passive: Boolean,
      durable: Boolean,
      autoDelete: Boolean,
      internal: Boolean,
      noWait: NoWait,
      arguments: FieldTable
  ) extends ExchangeClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(10)
  }

  case object DeclareOk
      extends ExchangeClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(11)
  }

  final case class Delete(
      exchange: ExchangeName,
      ifUnused: Boolean,
      noWait: NoWait
  ) extends ExchangeClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(20)
  }

  case object DeleteOk
      extends ExchangeClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(21)
  }

  final case class Bind(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait,
      arguments: FieldTable
  ) extends ExchangeClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(30)
  }

  case object BindOk
      extends ExchangeClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(31)
  }

  final case class Unbind(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait,
      arguments: FieldTable
  ) extends ExchangeClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(40)
  }

  case object UnbindOk
      extends ExchangeClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(51)
  }
}

sealed trait QueueClass extends Method {
  override val _classId = ClassId(50)
}

object QueueClass {

  final case class Declare(
      queue: QueueName,
      passive: Boolean,
      durable: Boolean,
      exclusive: Boolean,
      autoDelete: Boolean,
      noWait: NoWait,
      arguments: FieldTable
  ) extends QueueClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(10)
  }

  final case class DeclareOk(
      queue: QueueName,
      messageCount: MessageCount,
      consumerCount: Int
  ) extends QueueClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(11)
  }

  final case class Bind(
      queue: QueueName,
      exchange: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait,
      arguments: FieldTable
  ) extends QueueClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(20)
  }

  case object BindOk
      extends QueueClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(21)
  }

  final case class Unbind(
      queue: QueueName,
      exchange: ExchangeName,
      routingKey: ShortString,
      arguments: FieldTable
  ) extends QueueClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(50)
  }

  case object UnbindOk
      extends QueueClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(51)
  }

  final case class Purge(queue: QueueName, noWait: NoWait)
      extends QueueClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(30)
  }

  final case class PurgeOk(messageCount: MessageCount)
      extends QueueClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(31)
  }

  final case class Delete(
      queue: QueueName,
      ifUnused: Boolean,
      ifEmpty: Boolean,
      noWait: NoWait
  ) extends QueueClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(40)
  }

  final case class DeleteOk(messageCount: MessageCount)
      extends QueueClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(41)
  }
}

sealed trait BasicClass extends Method {
  override val _classId = ClassId(60)
}

object BasicClass {

  final case class Qos(prefetchSize: Int, prefetchCount: Short, global: Boolean)
      extends BasicClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(10)
  }

  case object QosOk extends BasicClass with Sync with NotRequest with Response {
    override val _methodId = MethodId(11)
  }

  final case class Consume(
      queue: QueueName,
      consumerTag: ConsumerTag,
      noLocal: NoLocal,
      noAck: NoAck,
      exclusive: Boolean,
      noWait: NoWait,
      arguments: FieldTable
  ) extends BasicClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(20)
  }

  final case class ConsumeOk(consumerTag: ConsumerTag)
      extends BasicClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(21)
  }

  final case class Cancel(consumerTag: ConsumerTag, noWait: NoWait)
      extends BasicClass
      with Sync
      with Request
      with Response {
    override val _methodId = MethodId(30)
  }

  final case class CancelOk(consumerTag: ConsumerTag)
      extends BasicClass
      with Sync
      with Request
      with Response {
    override val _methodId = MethodId(31)
  }

  final case class Publish(
      exchange: ExchangeName,
      routingKey: ShortString,
      mandatory: Boolean,
      immediate: Boolean
  ) extends BasicClass
      with Async
      with Request
      with NotResponse {
    override val _methodId = MethodId(40)
  }

  final case class Return(
      replyCode: ReplyCode,
      replyText: ReplyText,
      exchange: ExchangeName,
      routingKey: ShortString
  ) extends BasicClass
      with Async
      with NotRequest
      with Response {
    override val _methodId = MethodId(50)
  }

  final case class Deliver(
      consumerTag: ConsumerTag,
      deliveryTag: DeliveryTag,
      redelivered: Redelivered,
      exchange: ExchangeName,
      routingKey: ShortString
  ) extends BasicClass
      with Async
      with NotRequest
      with Response {
    override val _methodId = MethodId(60)
  }

  final case class Get(queue: QueueName, noAck: NoAck)
      extends BasicClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(70)
  }

  final case class GetOk(
      deliveryTag: DeliveryTag,
      redelivered: Redelivered,
      exchange: ExchangeName,
      routingKey: ShortString,
      messageCount: MessageCount
  ) extends BasicClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(71)
  }

  case object GetEmpty
      extends BasicClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(72)
  }

  final case class Ack(deliveryTag: DeliveryTag, multiple: Boolean)
      extends BasicClass
      with Async
      with Request
      with Response {
    override val _methodId = MethodId(80)
  }

  final case class Reject(deliveryTag: DeliveryTag, requeue: Boolean)
      extends BasicClass
      with Async
      with Request
      with NotResponse {
    override val _methodId = MethodId(90)
  }

  final case class RecoverAsync(requeue: Boolean)
      extends BasicClass
      with Async
      with Request
      with NotResponse {
    override val _methodId = MethodId(100)
  }

  final case class Recover(requeue: Boolean)
      extends BasicClass
      with Async
      with Request
      with NotResponse {
    override val _methodId = MethodId(110)
  }

  case object RecoverOk
      extends BasicClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(111)
  }

  final case class Nack(
      deliveryTag: DeliveryTag,
      multiple: Boolean,
      requeue: Boolean
  ) extends BasicClass
      with Async
      with Request
      with Response {
    override val _methodId = MethodId(120)
  }
}

sealed trait TxClass extends Method {
  override val _classId = ClassId(90)
}

object TxClass {

  case object Select extends TxClass with Sync with Request with NotResponse {
    override val _methodId = MethodId(10)
  }

  case object SelectOk extends TxClass with Sync with NotRequest with Response {
    override val _methodId = MethodId(11)
  }

  case object Commit extends TxClass with Sync with Request with NotResponse {
    override val _methodId = MethodId(20)
  }

  case object CommitOk extends TxClass with Sync with NotRequest with Response {
    override val _methodId = MethodId(21)
  }

  case object Rollback extends TxClass with Sync with Request with NotResponse {
    override val _methodId = MethodId(30)
  }

  case object RollbackOk
      extends TxClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(31)
  }
}

sealed trait ConfirmClass extends Method {
  override val _classId = ClassId(85)
}

object ConfirmClass {

  final case class Select(noWait: NoWait)
      extends ConfirmClass
      with Sync
      with Request
      with NotResponse {
    override val _methodId = MethodId(10)
  }

  case object SelectOk
      extends ConfirmClass
      with Sync
      with NotRequest
      with Response {
    override val _methodId = MethodId(11)
  }
}
