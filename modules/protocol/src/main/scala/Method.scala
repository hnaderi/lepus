package lepus.protocol

import lepus.protocol.domains.*

trait Class(val _classId: ClassId)
trait Method(val _methodId: MethodId) extends Class
trait Request
trait Response
