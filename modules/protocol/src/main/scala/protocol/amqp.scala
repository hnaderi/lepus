package lepus.protocol

import java.time.Instant

object AMQP {
  trait Method(val methodId: Short)
  trait SynchronousMethod extends Method
  trait AsynchronousMethod extends Method

  trait Class(val classId: Short)
}
