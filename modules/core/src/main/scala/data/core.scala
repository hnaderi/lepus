package lepus.core

import java.time.Instant

opaque type ExchangeName = String
opaque type QueueName = String
opaque type RoutingKey = String
opaque type ChannelNumber <: Short = Short
object ChannelNumber {
  def apply(n: Short): Option[ChannelNumber] = Option.when(n >= 0)(n)
}
opaque type ConnectionId = String
opaque type ExchangeType <: String = "direct" | "fanout" | "topic" | "header"
object ExchangeType {
  def apply(str: String): Option[ExchangeType] = str match {
    case s: ExchangeType => Some(s)
    case _               => None
  }
}
type QDeclareOK
type BindArgs
opaque type DeliveryTag = Long
opaque type ConsumerTag = String
opaque type PublishSeqNr = Long

opaque type ShortString <: String = String
object ShortString {
  def apply(str: String): ShortString = str
}

type FieldTable = Map[ShortString, FieldValue]

type AMQPField = Long | Int | Short | ShortString | String | Instant |
  FieldTable

type FieldValue = Boolean | Short | UnsignedShort | Int | UnsignedInt | Long |
  UnsignedLong | Float | Double | BigDecimal | ShortString

opaque type UnsignedShort = Short
opaque type UnsignedInt = Int
opaque type UnsignedLong = Long

opaque type ClassId = Int
opaque type MethodId = Int

case object Heartbeat
type Heartbeat = Heartbeat.type

final case class FrameProperties(channel: ChannelNumber, size: Long)
type PropertyFlags
final case class HeaderPayload(
    contentClass: Any,
    bodySize: Long,
    propertyFlags: PropertyFlags,
    properties: List[AMQPField]
)
final case class ContentHeader(
    properties: FrameProperties,
    payload: HeaderPayload
)

final case class ContentBody(properties: FrameProperties, payload: Array[Byte])

final case class Content(header: ContentHeader, body: Option[ContentBody])

type AMQPUnit = Method | Content | ContentBody | Heartbeat
