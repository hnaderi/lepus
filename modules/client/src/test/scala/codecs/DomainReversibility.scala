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

class DomainReversibility extends munit.ScalaCheckSuite {

  def assertReversed[T](
      original: T,
      obtained: Attempt[DecodeResult[T]]
  )(using Location): Unit =
    obtained.toEither
      .map(v => assertEquals(original, v.value))
      .leftMap(e => fail(e.messageWithContext))
      .merge

  property("Field tables") {
    forAll(fieldTable) { table =>
      val res = for {
        enc <- DomainCodecs.fieldTable.encode(table)
        dec <- DomainCodecs.fieldTable.decode(enc)
      } yield dec

      assertReversed(table, res)
    }
  }

  property("Short string") {
    forAll(shortString) { shs =>
      val res = for {
        enc <- DomainCodecs.shortString.encode(shs)
        dec <- DomainCodecs.shortString.decode(enc)
      } yield dec

      assertReversed(shs, res)
    }
  }

  property("Long string") {
    forAll(longString) { longStr =>
      val res = for {
        enc <- DomainCodecs.longString.encode(longStr)
        dec <- DomainCodecs.longString.decode(enc)
      } yield dec

      assertReversed(longStr, res)
    }
  }

  property("exchange name") {
    forAll(exchangeName) { exchName =>
      val res = for {
        enc <- DomainCodecs.exchangeName.encode(exchName)
        dec <- DomainCodecs.exchangeName.decode(enc)
      } yield dec

      assertReversed(exchName, res)
    }
  }

  property("queue name") {
    forAll(queueName) { qName =>
      val res = for {
        enc <- DomainCodecs.queueName.encode(qName)
        dec <- DomainCodecs.queueName.decode(enc)
      } yield dec

      assertReversed(qName, res)
    }
  }

  property("path") {
    forAll(path) { p =>
      val res = for {
        enc <- DomainCodecs.path.encode(p)
        dec <- DomainCodecs.path.decode(enc)
      } yield dec

      assertReversed(p, res)
    }
  }

  property("delivery mode") {
    forAll(deliveryMode) { d =>
      val res = for {
        enc <- DomainCodecs.deliveryMode.encode(d)
        dec <- DomainCodecs.deliveryMode.decode(enc)
      } yield dec

      assertReversed(d, res)
    }
  }

  property("timestamp") {
    forAll(timestamp) { t =>
      val res = for {
        enc <- DomainCodecs.timestamp.encode(t)
        dec <- DomainCodecs.timestamp.decode(enc)
      } yield dec

      assertReversed(t, res)
    }
  }

  property("field data") {
    forAll(fieldData) { fd =>
      val res = for {
        enc <- DomainCodecs.fieldData.encode(fd)
        dec <- DomainCodecs.fieldData.decode(enc)
      } yield dec

      assertReversed(fd, res)
    }
  }

  property("reply code") {
    forAll(replyCode) { rc =>
      val res = for {
        enc <- DomainCodecs.replyCode.encode(rc)
        dec <- DomainCodecs.replyCode.decode(enc)
      } yield dec

      assertReversed(rc, res)
    }
  }

}
