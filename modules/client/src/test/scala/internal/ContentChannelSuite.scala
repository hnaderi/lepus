/*
 * Copyright 2021 Hossein Naderi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lepus.client
package internal

import cats.effect.IO
import cats.effect.testkit.TestControl
import cats.implicits.*
import lepus.codecs.BasicDataGenerator
import lepus.codecs.DomainGenerators
import lepus.protocol.*
import lepus.protocol.classes.basic.Properties
import lepus.protocol.domains.*
import munit.CatsEffectSuite
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.effect.PropF
import org.scalacheck.effect.PropF.*
import scodec.bits.ByteVector

import ContentChannel.*
import ContentChannelSuite.*

class ContentChannelSuite extends InternalTestSuite {

  test("Must fail when receives header before starting") {
    forAllF(incomingContent) { content =>
      for {
        sut <- newChannel()
        _ <- sut.cc
          .recv(content.header)
          .intercept[AMQPError]
      } yield ()
    }
  }

  test("Must fail when receives header before content") {
    forAllF(asyncNotifs, incomingContent.suchThat(!_.bodies.isEmpty)) {
      (m, content) =>
        for {
          sut <- newChannel()
          _ <- sut.cc.asyncNotify(m)
          _ <- sut.cc
            .recv(content.bodies.head)
            .intercept[AMQPError]
        } yield ()
    }
  }

  test("Must recieve and merge all async deliveries") {
    forAllF(asyncContents) { contents =>
      for {
        sut <- newChannel()
        _ <- contents.traverse((m, content) =>
          assertAsyncContent(m, content, sut)
        )
      } yield ()
    }
  }

  // TODO this test can be made more realistic
  test("Must skip async deliveries on abort") {
    val abortionScenario = for {
      a <- asyncContent
      totalActions = 2 + a._2.bodies.size
      splittingSize <- Gen.chooseNum(1, totalActions - 1)
      f = (cc: ContentChannel[IO]) => {
        val acs = asyncActions(cc)(a._1, a._2)
        val beforeAbort = acs.slice(0, splittingSize).map(_.assertEquals(()))
        val afterAbort = acs
          .slice(splittingSize, totalActions)
          .map(_.intercept[AMQPError])

        (beforeAbort ::: cc.abort :: afterAbort).sequence.void
      }
    } yield f
    val abortionScenarios = list(abortionScenario)

    forAllF(abortionScenarios) { scenarios =>
      for {
        sut <- newChannel()
        _ <- scenarios.traverse(_(sut.cc))
        _ <- sut.dispatcher.assertNoContent
      } yield ()
    }
  }

  test("Must recieve and merge all sync deliveries") {
    forAllF(syncContents) { contents =>
      TestControl.executeEmbed(
        for {
          sut <- newChannel(size = 1)
          _ <- contents.traverse((req, res, content) =>
            assertSyncContent(req, res, content, sut.cc)
          )
        } yield ()
      )
    }
  }

  test(
    "Must recieve None in synchronous get, when server responde with GetEmpty"
  ) {
    forAllF(list(BasicDataGenerator.getGen)) { ms =>
      TestControl.executeEmbed(
        for {
          sut <- newChannel(size = 1)
          _ <- ms.traverse(m =>
            sut.cc
              .get(m)
              .flatMap(d =>
                d.tryGet.assertEquals(None) >>
                  sut.cc.syncNotify(BasicClass.GetEmpty) >>
                  d.get.assertEquals(None)
              )
          )
        } yield ()
      )
    }
  }

  private def asyncActions(cc: ContentChannel[IO])(
      method: ContentMethod,
      content: IncomingContent
  ): List[IO[Unit]] =
    List(cc.asyncNotify(method)) ::: contentActions(cc)(content)

  private def syncActions(cc: ContentChannel[IO])(
      method: BasicClass.GetOk,
      content: IncomingContent
  ): List[IO[Unit]] =
    List(cc.syncNotify(method)) ::: contentActions(cc)(content)

  private def contentActions(cc: ContentChannel[IO])(content: IncomingContent) =
    List(cc.recv(content.header)) ::: content.bodies.map(cc.recv)

  private def assertAsyncContent(
      method: ContentMethod,
      content: IncomingContent,
      cc: SUT
  ) = for {
    _ <- asyncActions(cc.cc)(method, content).traverse(_.assertEquals(()))
    msg = MessageRaw(content.payload, content.properties)
    _ <- method match {
      case m: BasicClass.Deliver =>
        cc.dispatcher.assertDelivered(
          DeliveredMessageRaw(
            consumerTag = m.consumerTag,
            deliveryTag = m.deliveryTag,
            redelivered = m.redelivered,
            exchange = m.exchange,
            routingKey = m.routingKey,
            msg
          )
        )
      case m: BasicClass.Return =>
        cc.dispatcher.assertReturned(
          ReturnedMessageRaw(
            replyCode = m.replyCode,
            replyText = m.replyText,
            exchange = m.exchange,
            routingKey = m.routingKey,
            msg
          )
        )
    }
  } yield ()

  private def assertSyncContent(
      m0: BasicClass.Get,
      m: BasicClass.GetOk,
      content: IncomingContent,
      cc: ContentChannel[IO]
  ) =
    for {
      msgDef <- cc.get(m0)
      _ <- msgDef.tryGet.assertEquals(None)
      _ <- syncActions(cc)(m, content).traverse(_.assertEquals(()))
      _ <- msgDef.get.assertEquals(
        SynchronousGetRaw(
          deliveryTag = m.deliveryTag,
          redelivered = m.redelivered,
          exchange = m.exchange,
          routingKey = m.routingKey,
          messageCount = m.messageCount,
          message = content.message
        ).some
      )
    } yield ()
}

object ContentChannelSuite {

  final case class SUT(
      dispatcher: FakeMessageDispatcher,
      cc: ContentChannel[IO]
  )

  private def newChannel(
      ch: ChannelNumber = ChannelNumber(1),
      size: Int = 0
  ) = for {
    pq <- FakeFrameOutput()
    s <- ChannelOutput(pq)
    gl <- Waitlist[IO, Option[SynchronousGetRaw]](size)
    fd <- FakeMessageDispatcher()
    cc <- ContentChannel[IO](
      ch,
      publisher = s,
      dispatcher = fd,
      gl
    )
  } yield SUT(fd, cc)

  val channel = DomainGenerators.channelNumber
  val binary = Gen
    .choose(0, 1000)
    .flatMap(n =>
      Gen
        .containerOfN[Array, Byte](n, Arbitrary.arbitrary[Byte])
        .map(ByteVector(_))
    )
  val maxSize = Gen.choose[Long](5, 100)
  val props = DomainGenerators.properties

  final case class IncomingContent(
      header: Frame.Header,
      bodies: List[Frame.Body]
  ) {
    def payload: ByteVector =
      bodies.map(_.payload).foldLeft(ByteVector.empty)(_ ++ _)
    def properties: Properties = header.props

    def message: MessageRaw = MessageRaw(payload, properties)
  }

  val incomingContent: Gen[IncomingContent] = for {
    ch <- channel
    p <- props
    ms <- maxSize
    data <- binary
  } yield IncomingContent(
    header = Frame.Header(ch, ClassId(10), data.size, p),
    bodies = List
      .range(0L, data.size, ms)
      .map(i => Frame.Body(ch, data.slice(i, i + ms)))
  )

  val asyncNotifs: Gen[ContentMethod] =
    Gen.oneOf(BasicDataGenerator.deliverGen, BasicDataGenerator.returnGen)

  val syncNotifs = BasicDataGenerator.getOkGen

  val anyNotifs: Gen[ContentMethod | BasicClass.GetOk] =
    Gen.oneOf(asyncNotifs, syncNotifs)

  val syncContent: Gen[(BasicClass.Get, BasicClass.GetOk, IncomingContent)] =
    for {
      g <- BasicDataGenerator.getGen
      m <- syncNotifs
      c <- incomingContent
    } yield (g, m, c)

  val syncContents = list(syncContent)

  val asyncContent: Gen[(ContentMethod, IncomingContent)] = for {
    m <- asyncNotifs
    c <- incomingContent
  } yield (m, c)

  val asyncContents = list(asyncContent)

  private def list[T](g: Gen[T]) =
    Gen.choose(1, 5).flatMap(n => Gen.listOfN(n, g))
}
