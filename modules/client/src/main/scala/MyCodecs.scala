package lepus.client.codecs

import scodec.{Codec, Encoder, Decoder}
import scodec.bits.*
import scodec.codecs
import scodec.codecs.*
import lepus.protocol.*
import lepus.protocol.frame.*
import lepus.protocol.domains.*

object MyCodecs {
  val amqpLiteral: Codec[Unit] = constantLenient(65, 77, 81, 80)
  val protocolId: Codec[Unit] = constant(hex"0")
  val protocolVersion: Codec[ProtocolVersion] =
    (uint8 :: uint8 :: uint8).as[ProtocolVersion]
  val protocol: Codec[ProtocolVersion] =
    (amqpLiteral ~> protocolId ~> protocolVersion).as[ProtocolVersion]

  val channelNumber: Codec[ChannelNumber] =
    short16.xmap(ChannelNumber(_), identity)

  val frameEnd: Codec[Unit] = constant(hex"CE")

  private val classId: Codec[ClassId] = short16.xmap(ClassId(_), identity)
  private val methodId: Codec[MethodId] = short16.xmap(MethodId(_), identity)
  private val byteArray: Codec[Array[Byte]] =
    bytes.xmap(_.toArray, ByteVector(_))

  private val methodFP: Codec[FramePayload.Method] =
    (classId :: methodId :: byteArray).as
  private val headerFP: Codec[FramePayload.Header] = (classId :: constant(
    hex"00"
  ) ~> long(64) :: provide[Array[Byte]](Array.empty) :: byteArray).as

  private val bodyFP: Codec[FramePayload.Body] = byteArray.as

  private val heartbeat: Codec[FramePayload.Heartbeat.type] =
    codecs.provide(FramePayload.Heartbeat)

  val frame: Codec[Frame] = discriminated
    .by(uint8)
    .typecase(1, channelNumber :: sized(methodFP))
    .typecase(2, channelNumber :: sized(headerFP))
    .typecase(3, channelNumber :: sized(bodyFP))
    .typecase(8, channelNumber :: sized(headerFP))
    .withContext("Frame")
    .as[Frame] <~ frameEnd

  private def sized[T](payload: Codec[T]): Codec[T] =
    variableSizeBytes(int(32), payload, 0)
}
