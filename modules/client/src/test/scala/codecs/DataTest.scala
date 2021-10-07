package lepus.codecs

import cats.implicits.*
import com.rabbitmq.client.impl.AMQImpl
import lepus.client.codecs.DomainCodecs
import lepus.client.codecs.ExchangeCodecs
import lepus.client.codecs.FrameCodec
import lepus.client.codecs.MethodCodec
import lepus.protocol.Method
import lepus.protocol.classes.ExchangeClass
import lepus.protocol.classes.QueueClass
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
import DataTest.*

class DataTest extends munit.ScalaCheckSuite {

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
      assertEquals(Attempt.successful[Method](d), lepusRead.map(_.value))
    }
  }

  property("Queue declare") {
    forAll(qDeclare) { d =>
      val df = AMQImpl.Queue.Declare(
        0,
        d.queue,
        d._2,
        d._3,
        d._4,
        d._5,
        d._6,
        java.util.Map.of()
      )
      val officialOut = writeMethod(df)
      val lepusRead = MethodCodec.all.decode(officialOut)
      val lepusOut = MethodCodec.all.encode(d).getOrElse(???)
      assertEquals(bitSplit(officialOut.toBin), bitSplit(lepusOut.toBin))
      assertEquals(Attempt.successful[Method](d), lepusRead.map(_.value))
    }
  }
}

object DataTest {
  val deletes: Gen[ExchangeClass.Delete] = for {
    ex <- exchangeName
    a <- Arbitrary.arbitrary[Boolean]
    b <- Arbitrary.arbitrary[Boolean]
  } yield ExchangeClass.Delete(ex, a, b)

  val qDeclare: Gen[QueueClass.Declare] = for {
    qName <- queueName
    a <- Arbitrary.arbitrary[Boolean]
    b <- Arbitrary.arbitrary[Boolean]
    c <- Arbitrary.arbitrary[Boolean]
    d <- Arbitrary.arbitrary[Boolean]
    e <- Arbitrary.arbitrary[Boolean]
  } yield QueueClass.Declare(qName, a, b, c, d, e, FieldTable.empty)
}
