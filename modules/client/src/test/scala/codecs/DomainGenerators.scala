package lepus.codecs

import lepus.client.codecs.DomainCodecs
import lepus.client.codecs.FrameCodec
import lepus.protocol.domains.*
import munit.FunSuite
import org.scalacheck.Prop._
import scodec.Codec
import scodec.Decoder
import scodec.Encoder
import scodec.bits.*
import scodec.codecs
import scodec.codecs.*
import lepus.protocol.classes.ExchangeClass
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import lepus.client.codecs.MethodCodec
import lepus.protocol.Method
import lepus.client.codecs.ExchangeCodecs
import com.rabbitmq.client.impl.AMQImpl
import scala.annotation.meta.field
import cats.implicits.*
import lepus.protocol.constants.ReplyCode

object DomainGenerators {
  extension [A](g: Gen[A]) {
    def emap[B](f: A => Either[String, B]): Gen[B] =
      g.flatMap(a => f(a).map(Gen.const).getOrElse(Gen.fail))
  }
  val exchangeName: Gen[ExchangeName] = Gen.alphaNumStr.emap(ExchangeName(_))
  val queueName: Gen[QueueName] = Gen.alphaNumStr.emap(QueueName(_))

  val methodIds: Gen[MethodId] = Arbitrary.arbitrary[Short].map(MethodId(_))
  val classIds: Gen[ClassId] = Arbitrary.arbitrary[Short].map(ClassId(_))

  val shortString: Gen[ShortString] = Gen
    .choose(0, 255)
    .flatMap(n => Gen.stringOfN(n, Gen.alphaNumChar).emap(ShortString(_)))
  val longString: Gen[LongString] =
    Gen.alphaNumStr.emap(LongString(_))

  val timestamp: Gen[Timestamp] = Arbitrary.arbitrary[Long].map(Timestamp(_))

  val messageCount: Gen[MessageCount] = Gen.posNum[Long].emap(MessageCount(_))
  val consumerTag: Gen[ConsumerTag] = shortString.map(ConsumerTag(_))
  val path: Gen[Path] = shortString.emap(Path(_))
  val deliveryTag: Gen[DeliveryTag] =
    Arbitrary.arbitrary[Long].map(DeliveryTag(_))

  val deliveryMode: Gen[DeliveryMode] =
    Gen.oneOf(DeliveryMode.Persistent, DeliveryMode.NonPersistent)
  val priority: Gen[Priority] = Gen.oneOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

  val decimal: Gen[Decimal] = for {
    scale <- Arbitrary.arbitrary[Byte]
    value <- Arbitrary.arbitrary[Int]
  } yield Decimal(scale, value)

  def fieldData: Gen[FieldData] = Gen.oneOf(
    shortString,
    longString,
    timestamp,
    decimal,
    Arbitrary.arbitrary[Boolean],
    Arbitrary.arbitrary[Byte],
    Arbitrary.arbitrary[Short],
    Arbitrary.arbitrary[Int],
    Arbitrary.arbitrary[Long],
    Arbitrary.arbitrary[Float],
    Arbitrary.arbitrary[Double]
  )

  private val fieldElem = for {
    k <- shortString
    v <- fieldData
  } yield (k, v)

  private val fieldMap = Gen.mapOf(fieldElem)

  def fieldTable: Gen[FieldTable] =
    Gen.recursive[FieldTable](ft =>
      Gen.choose(1, 7).flatMap { n =>
        if n > 1 then
          for {
            k <- shortString
            v <- fieldMap
            nest = FieldTable(v)
            parent <- ft
          } yield parent.copy(values = parent.values.updated(k, nest))
        else fieldMap.map(FieldTable(_))
      }
    )

  val replyCode: Gen[ReplyCode] = Gen.oneOf(ReplyCode.values.toSeq)
}
