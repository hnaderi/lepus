package lepus.client.codecs

import scodec.*
import scodec.bits.*
import scodec.codecs.*
import lepus.core.*

val amqpLiteral: Codec[Unit] = constantLenient(65, 77, 81, 80)
val protocolId: Codec[Unit] = constant(hex"0")
val protocolVersion: Codec[ProtocolVersion] =
  (uint8 :: uint8 :: uint8).as[ProtocolVersion]
val protocol: Codec[ProtocolVersion] =
  (amqpLiteral ~> protocolId ~> protocolVersion).as[ProtocolVersion]

val frameType: Codec[FrameType] = mappedEnum(
  uint8,
  Map(
    FrameType.Method -> 1,
    FrameType.Header -> 2,
    FrameType.Body -> 3,
    FrameType.HeartBeat -> 8
  )
)
val channelNumber: Codec[ChannelNumber] = short16.exmap(
  i => Attempt.fromOption(ChannelNumber(i), Err(s"Invalid channel number $i")),
  Attempt.successful(_)
)
val frameEnd: Codec[Unit] = constant(hex"CE")

val frame: Decoder[Frame] = for {
  ft <- frameType
  ch <- channelNumber
  size <- int(32)
  payload <- bytes(size) <~ frameEnd
} yield Frame(ft, ch, payload.toArray)

// val amqpUnit: Codec[AMQPUnit] = ???
