package lepus.protocol.method

import lepus.protocol.domains.*
import lepus.protocol.constants.*

enum Basic {
  case Qos(prefetchSize: Int, prefetchCount: Short, global: Boolean)
  case QosOk()
  case Consume(
      reserved1: Unit,
      queue: QueueName,
      consumerTag: ConsumerTag,
      noLocal: NoLocal,
      noAck: NoAck,
      exclusive: Boolean,
      noWait: NoWait,
      arguments: FieldTable
  )
  case ConsumeOk(consumerTag: ConsumerTag)
  case Cancel(consumerTag: ConsumerTag, noWait: NoWait)
  case CancelOk(consumerTag: ConsumerTag)
  case Publish(
      reserved1: Unit,
      exchange: ExchangeName,
      routingKey: ShortString,
      mandatory: Boolean,
      immediate: Boolean
  )
  case Return(
      replyCode: ReplyCode,
      replyText: ReplyText,
      exchange: ExchangeName,
      routingKey: ShortString
  )
  case Deliver(
      consumerTag: ConsumerTag,
      deliveryTag: DeliveryTag,
      redelivered: Redelivered,
      exchange: ExchangeName,
      routingKey: ShortString
  )
  case Get(reserved1: Unit, queue: QueueName, noAck: NoAck)
  case GetOk(
      deliveryTag: DeliveryTag,
      redelivered: Redelivered,
      exchange: ExchangeName,
      routingKey: ShortString,
      messageCount: MessageCount
  )
  case GetEmpty(reserved1: Unit)
  case Ack(deliveryTag: DeliveryTag, multiple: Boolean)
  case Reject(deliveryTag: DeliveryTag, requeue: Boolean)
  case RecoverAsync(requeue: Boolean)
  case Recover(requeue: Boolean)
  case RecoverOk()
}
