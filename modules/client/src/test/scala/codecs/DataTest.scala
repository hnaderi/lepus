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

  def assertReversed[T](
      original: T,
      obtained: Attempt[DecodeResult[T]]
  )(using Location): Unit =
    obtained.toEither
      .map(v => assertEquals(original, v.value))
      .leftMap(e => fail(e.messageWithContext))
      .merge

  property("Field tables reversible") {
    forAll(fieldTable) { table =>
      val res = for {
        enc <- DomainCodecs.fieldTable.encode(table)
        dec <- DomainCodecs.fieldTable.decode(enc)
      } yield dec

      assertReversed(table, res)
    }
  }

  // property("Field tables official") {
  //   forAll(fieldTable) { table =>
  //     val res = DomainCodecs.fieldTable.encode(table).map(_.toHex)
  //     val official = writeMap(table).toHex

  //     res.toEither.map(assertEquals(_, official)).getOrElse(fail(""))
  //   }
  // }

  property("Short string reversible") {
    forAll(shortString) { shs =>
      val res = for {
        enc <- DomainCodecs.shortString.encode(shs)
        dec <- DomainCodecs.shortString.decode(enc)
      } yield dec

      assertReversed(shs, res)
    }
  }

  property("Long string reversible") {
    forAll(longString) { longStr =>
      val res = for {
        enc <- DomainCodecs.longString.encode(longStr)
        dec <- DomainCodecs.longString.decode(enc)
      } yield dec

      assertReversed(longStr, res)
    }
  }

  property("exchange name reversible") {
    forAll(exchangeName) { exchName =>
      val res = for {
        enc <- DomainCodecs.exchangeName.encode(exchName)
        dec <- DomainCodecs.exchangeName.decode(enc)
      } yield dec

      assertReversed(exchName, res)
    }
  }

  property("queue name reversible") {
    forAll(queueName) { qName =>
      val res = for {
        enc <- DomainCodecs.queueName.encode(qName)
        dec <- DomainCodecs.queueName.decode(enc)
      } yield dec

      assertReversed(qName, res)
    }
  }

  property("path reversible") {
    forAll(path) { p =>
      val res = for {
        enc <- DomainCodecs.path.encode(p)
        dec <- DomainCodecs.path.decode(enc)
      } yield dec

      assertReversed(p, res)
    }
  }

  property("delivery mode reversible") {
    forAll(deliveryMode) { d =>
      val res = for {
        enc <- DomainCodecs.deliveryMode.encode(d)
        dec <- DomainCodecs.deliveryMode.decode(enc)
      } yield dec

      assertReversed(d, res)
    }
  }

  property("timestamp reversible") {
    forAll(timestamp) { t =>
      val res = for {
        enc <- DomainCodecs.timestamp.encode(t)
        dec <- DomainCodecs.timestamp.decode(enc)
      } yield dec

      assertReversed(t, res)
    }
  }

  property("field data reversible") {
    forAll(fieldData) { fd =>
      val res = for {
        enc <- DomainCodecs.fieldData.encode(fd)
        dec <- DomainCodecs.fieldData.decode(enc)
      } yield dec

      assertReversed(fd, res)
    }
  }

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
