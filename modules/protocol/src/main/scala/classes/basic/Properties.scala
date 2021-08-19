package lepus.protocol.classes.basic

import lepus.protocol.domains.*
import lepus.protocol.constants.*

final case class Properties(
    contentType: Option[ShortString],
    contentEncoding: Option[ShortString],
    headers: FieldTable,
    deliveryMode: Option[Byte],
    priority: Option[Byte],
    correlationId: Option[ShortString],
    replyTo: Option[ShortString],
    expiration: Option[ShortString],
    messageId: Option[ShortString],
    timestamp: Option[Timestamp],
    msgType: Option[ShortString],
    userId: Option[ShortString],
    appId: Option[ShortString]
    // clusterId: Option[String]
)
