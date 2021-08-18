package lepus.core

enum ConsumerEvent {
  case ConsumeOk(tag: ConsumerTag)
  case CancelOk(tag: ConsumerTag)
  case Cancel(tag: ConsumerTag)
  case Shutdown(tag: ConsumerTag, signal: String)
  case RecoverOk(tag: ConsumerTag)
  case Delivery(tag: ConsumerTag, msg: Message)
}
