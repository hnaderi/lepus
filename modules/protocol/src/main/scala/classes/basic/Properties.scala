package lepus.protocol.classes.basic

import lepus.protocol.domains.*
import lepus.protocol.constants.*

final case class Properties(
    contentType: Option[ShortString] = None,
    contentEncoding: Option[ShortString] = None,
    headers: Option[FieldTable] = None,
    deliveryMode: Option[DeliveryMode] = None,
    priority: Option[Priority] = None,
    correlationId: Option[ShortString] = None,
    replyTo: Option[ShortString] = None,
    expiration: Option[ShortString] = None,
    messageId: Option[ShortString] = None,
    timestamp: Option[Timestamp] = None,
    msgType: Option[ShortString] = None,
    userId: Option[ShortString] = None,
    appId: Option[ShortString] = None,
    clusterId: Option[ShortString] = None
)
