package lepus.client.codecs

import scodec.{Codec, Encoder, Decoder}
import scodec.bits.*
import scodec.codecs
import scodec.codecs.*
import lepus.protocol.*
import lepus.protocol.frame.*
import lepus.protocol.domains.*
import java.nio.ByteBuffer
import com.rabbitmq.client.impl.ValueWriter

object FrameCodec {
  import DomainCodecs.*
  private lazy val amqpLiteral: Codec[Unit] = constant('A', 'M', 'Q', 'P')
  private lazy val protocolId: Codec[Unit] = byte.unit(0)
  private lazy val protocolVersion: Codec[ProtocolVersion] =
    (int8 :: int8 :: int8).as[ProtocolVersion]
  lazy val protocol: Codec[ProtocolVersion] =
    (amqpLiteral ~> protocolId ~> protocolVersion).as[ProtocolVersion]

  lazy val frameEnd: Codec[Unit] = constant(hex"CE")

  private lazy val byteArray: Codec[ByteBuffer] =
    bytes.xmap(_.toByteBuffer, ByteVector(_))

  private val methodFP: Codec[Frame.Method] =
    (channelNumber :: sized(MethodCodec.all)).as

  private val headerFP: Codec[Frame.Header] =
    (channelNumber :: sized(
      classId :: short16.unit(0) ~> int64 :: basicProps
    )).as

  private val bodyFP: Codec[Frame.Body] =
    (channelNumber :: sized(byteArray)).as

  private val heartbeat: Codec[Frame.Heartbeat.type] =
    channelNumber.unit(ChannelNumber(0)) ~> codecs.provide(Frame.Heartbeat)

  lazy val frame: Codec[Frame] = discriminated
    .by(int8)
    .typecase(1, methodFP)
    .typecase(2, headerFP)
    .typecase(3, bodyFP)
    .typecase(8, headerFP)
    .withContext("Frame")
    .as[Frame] <~ frameEnd

  private def sized[T](payload: Codec[T]): Codec[T] =
    variableSizeBytes(int(32), payload)
}
