package lepus.protocol.method

import lepus.protocol.domains.*
import lepus.protocol.constants.*

enum Connection {
  case Start(
      versionMajor: Byte,
      versionMinor: Byte,
      serverProperties: PeerProperties,
      mechanisms: LongString,
      locales: LongString
  )
  case StartOk(
      clientProperties: PeerProperties,
      mechanism: ShortString,
      response: LongString,
      locale: ShortString
  )
  case Secure(challenge: LongString)
  case SecureOk(response: LongString)
  case Tune(channelMax: Short, frameMax: Int, heartbeat: Short)
  case TuneOk(channelMax: Short, frameMax: Int, heartbeat: Short)
  case Open(virtualHost: Path, reserved1: Unit, reserved2: Unit)
  case OpenOk(reserved1: Unit)
  case Close(
      replyCode: ReplyCode,
      replyText: ReplyText,
      classId: ClassId,
      methodId: MethodId
  )
  case CloseOk()
}
