package lepus.core

final case class Frame(
    frameType: FrameType,
    channel: ChannelNumber,
    payload: Array[Byte]
)

final case class Frame2(
    channel: ChannelNumber,
    payload: FramePayload
)

enum FramePayload {
  case Method(methodId: Short, classId: Short)
  case Header()
  case Body()
  case Heartbeat
}
