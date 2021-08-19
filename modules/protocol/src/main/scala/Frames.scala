package lepus.protocol.frame

import lepus.protocol.domains.*

final case class Frame(channel: ChannelNumber, payload: FramePayload)

enum FramePayload {
  case Method(classId: ClassId, methodId: MethodId, payload: Array[Byte])
  case Header(
      classId: ClassId,
      bodySize: Long,
      propertyFlags: Array[Byte],
      properties: Array[Byte]
  )
  case Body(payload: Array[Byte])
  case Heartbeat
}
