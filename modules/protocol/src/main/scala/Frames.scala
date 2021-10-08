package lepus.protocol.frame

import lepus.protocol.domains.*
import lepus.protocol.classes.basic.Properties
import java.nio.ByteBuffer

enum Frame {
  case Method(channel: ChannelNumber, value: lepus.protocol.Method)
  case Header(
      channel: ChannelNumber,
      classId: ClassId,
      bodySize: Long,
      props: Properties
  )
  case Body(channel: ChannelNumber, payload: ByteBuffer)
  case Heartbeat
}
