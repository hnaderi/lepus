package lepus.codecs

import cats.implicits.*
import com.rabbitmq.client.impl.AMQImpl
import lepus.client.codecs.DomainCodecs
import lepus.client.codecs.ExchangeCodecs
import lepus.client.codecs.FrameCodec
import lepus.client.codecs.MethodCodec
import lepus.protocol.Method
import lepus.protocol.classes.ExchangeClass
import lepus.protocol.domains.*
import munit.FunSuite
import munit.Location
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Prop._
import scodec.*
import scodec.bits.*
import scodec.codecs.*

import DomainGenerators.*
import lepus.protocol.frame.Frame
import lepus.protocol.frame.Frame
import java.nio.ByteBuffer

class FramesTest extends CodecTest {

  override def scalaCheckTestParameters =
    super.scalaCheckTestParameters
      .withMinSuccessfulTests(2000)
      .withMaxDiscardRatio(10)

  val method: Gen[Frame.Method] = for {
    ch <- channelNumber
    vl <- AllClassesDataGenerator.methods
  } yield Frame.Method(ch, vl)

  val header: Gen[Frame.Header] = for {
    ch <- channelNumber
    cls <- classIds
    size <- Gen.posNum[Long]
    props <- properties
  } yield Frame.Header(ch, cls, size, props)

  val body: Gen[Frame.Body] = for {
    ch <- channelNumber
    pl <- Gen.asciiStr.map(_.getBytes).map(ByteBuffer.wrap)
  } yield Frame.Body(ch, pl)

  val heartbeat: Gen[Frame.Heartbeat.type] =
    Gen.const(Frame.Heartbeat)

  val frames: Gen[Frame] =
    Gen.oneOf(method, header, body, header)

  property("All frame codecs must be reversible") {
    forAll(frames) { f =>
      val res = for {
        enc <- FrameCodec.frame.encode(f)
        dec <- FrameCodec.frame.decode(enc)
      } yield dec

      assertReversed(f, res)
    }
  }

}
