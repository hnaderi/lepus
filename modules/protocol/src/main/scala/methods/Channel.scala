package lepus.protocol.method

import lepus.protocol.domains.*
import lepus.protocol.constants.*

enum Channel {
  case Open(reserved1: Unit)
  case OpenOk(reserved1: Unit)
  case Flow(active: Boolean)
  case FlowOk(active: Boolean)
  case Close(
      replyCode: ReplyCode,
      replyText: ReplyText,
      classId: ClassId,
      methodId: MethodId
  )
  case CloseOk()
}
