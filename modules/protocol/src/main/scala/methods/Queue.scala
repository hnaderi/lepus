package lepus.protocol.method

import lepus.protocol.domains.*
import lepus.protocol.constants.*

enum Queue {
  case Declare(
      reserved1: Unit,
      queue: QueueName,
      passive: Boolean,
      durable: Boolean,
      exclusive: Boolean,
      autoDelete: Boolean,
      noWait: NoWait,
      arguments: FieldTable
  )
  case DeclareOk(
      queue: QueueName,
      messageCount: MessageCount,
      consumerCount: Int
  )
  case Bind(
      reserved1: Unit,
      queue: QueueName,
      exchange: ExchangeName,
      routingKey: ShortString,
      noWait: NoWait,
      arguments: FieldTable
  )
  case BindOk()
  case Unbind(
      reserved1: Unit,
      queue: QueueName,
      exchange: ExchangeName,
      routingKey: ShortString,
      arguments: FieldTable
  )
  case UnbindOk()
  case Purge(reserved1: Unit, queue: QueueName, noWait: NoWait)
  case PurgeOk(messageCount: MessageCount)
  case Delete(
      reserved1: Unit,
      queue: QueueName,
      ifUnused: Boolean,
      ifEmpty: Boolean,
      noWait: NoWait
  )
  case DeleteOk(messageCount: MessageCount)
}
