package lepus.protocol.frame

import lepus.protocol.domains.*
import lepus.protocol.classes.basic.Properties

final case class Frame(channel: ChannelNumber, payload: FramePayload)

enum FramePayload {
  case Method(value: lepus.protocol.Method)
  case Header(
      classId: ClassId,
      bodySize: Long,
      props: Properties
  )
  case Body(payload: Array[Byte])
  case Heartbeat
}
