package lepus.core

import java.time.Instant

final case class BasicProperties(
    contentType: Option[String],
    contentEncoding: Option[String],
    headers: Map[String, HeaderValue],
    deliveryMode: Option[Integer],
    priority: Option[Integer],
    correlationId: Option[String],
    replyTo: Option[String],
    expiration: Option[String],
    messageId: Option[String],
    timestamp: Option[Instant],
    msgType: Option[String],
    userId: Option[String],
    appId: Option[String],
    clusterId: Option[String]
)
