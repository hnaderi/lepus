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

class DataTest extends munit.ScalaCheckSuite {

  val deletes: Gen[ExchangeClass.Delete] = for {
    ex <- exchangeName
    a <- Arbitrary.arbitrary[Boolean]
    b <- Arbitrary.arbitrary[Boolean]
  } yield ExchangeClass.Delete(ex, a, b)

  // property("Field tables official") {
  //   forAll(fieldTable) { table =>
  //     val res = DomainCodecs.fieldTable.encode(table).map(_.toHex)
  //     val official = writeMap(table).toHex

  //     res.toEither.map(assertEquals(_, official)).getOrElse(fail(""))
  //   }
  // }

  property("Deletes") {
    forAll(deletes) { d =>
      val df = AMQImpl.Exchange.Delete(0, d.exchange, d.ifUnused, d.noWait)
      val officialOut = writeMethod(df)
      val lepusRead = MethodCodec.all.decode(officialOut)
      val lepusOut = MethodCodec.all.encode(d).getOrElse(???)
      assertEquals(bitSplit(officialOut.toBin), bitSplit(lepusOut.toBin))
    }
  }
}
