package lepus.client.codecs

import scodec.{Codec, Encoder, Decoder}
import scodec.bits.*
import scodec.codecs
import scodec.codecs.*
import lepus.protocol.*
import lepus.protocol.frame.*
import lepus.protocol.domains.*

object FrameCodec {
  import DomainCodecs.*
  lazy val amqpLiteral: Codec[Unit] = constantLenient(65, 77, 81, 80)
  lazy val protocolId: Codec[Unit] = constant(hex"0")
  lazy val protocolVersion: Codec[ProtocolVersion] =
    (int8 :: int8 :: int8).as[ProtocolVersion]
  lazy val protocol: Codec[ProtocolVersion] =
    (amqpLiteral ~> protocolId ~> protocolVersion).as[ProtocolVersion]

  lazy val frameEnd: Codec[Unit] = constant(hex"CE")

  private lazy val byteArray: Codec[Array[Byte]] =
    bytes.xmap(_.toArray, ByteVector(_))

  private lazy val methodFP: Codec[FramePayload.Method] = (MethodCodec.all).as
  private lazy val headerFP: Codec[FramePayload.Header] =
    (classId :: short16.unit(0) ~> long(64) :: basicProps).as

  private lazy val bodyFP: Codec[FramePayload.Body] = byteArray.as

  private lazy val heartbeat: Codec[FramePayload.Heartbeat.type] =
    codecs.provide(FramePayload.Heartbeat)

  lazy val frame: Codec[Frame] = discriminated
    .by(int8)
    .typecase(1, channelNumber :: sized(methodFP))
    .typecase(2, channelNumber :: sized(headerFP))
    .typecase(3, channelNumber :: sized(bodyFP))
    .typecase(8, channelNumber :: sized(headerFP))
    .withContext("Frame")
    .as[Frame] <~ frameEnd

  private def sized[T](payload: Codec[T]): Codec[T] =
    variableSizeBytes(int(32), payload)
}
