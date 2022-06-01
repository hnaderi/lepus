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

// package lepus.client
// package internal

// import cats.effect.IO
// import cats.effect.std.Queue
// import cats.implicits.*
// import lepus.codecs.BasicDataGenerator
// import lepus.codecs.DomainGenerators
// import lepus.protocol.*
// import lepus.protocol.classes.basic.Properties
// import lepus.protocol.constants.ReplyCode
// import lepus.protocol.domains.*
// import munit.CatsEffectSuite
// import munit.ScalaCheckEffectSuite
// import org.scalacheck.Arbitrary
// import org.scalacheck.Gen
// import org.scalacheck.effect.PropF
// import org.scalacheck.effect.PropF.*
// import scodec.bits.ByteVector

// import scala.concurrent.duration.*

// import ContentChannel.*
// import ContentChannelSuite.*

// class ContentChannelSuite extends CatsEffectSuite, ScalaCheckEffectSuite {
//   override def munitTimeout = 5.second

//   test("Must split publish data to frames with maximum permitted size") {
//     forAllNoShrinkF(
//       BasicDataGenerator.publishGen,
//       channel,
//       maxSize,
//       binary,
//       props
//     ) { (publishMethod, ch, size, data, props) =>
//       val frameCount = Math.ceil(data.size.toDouble / size.toDouble).toInt
//       for {
//         sut <- newChannel(ch, size, 1000)
//         _ <- sut.cc.send(publishMethod, Message(data, props))

//         _ <- sut.pq.size.assertEquals(frameCount + 2)

//         _ <- sut.pq.take.assertEquals(Frame.Method(ch, publishMethod))
//         _ <- sut.pq.take.assertEquals(
//           Frame.Header(
//             ch,
//             ClassId(10),
//             bodySize = data.size,
//             props
//           )
//         )
//         _ <- sut.pq.size.assertEquals(frameCount)

//         all <- (1 to frameCount).toList.traverse(_ => sut.pq.take)

//         expected = Range
//           .Long(0, data.size, size)
//           .map(i => Frame.Body(ch, data.slice(i, i + size)))
//           .toList

//       } yield {
//         assertEquals(all, expected)
//         val merged = all
//           .collect { case Frame.Body(_, pl) =>
//             pl
//           }
//           .foldLeft(ByteVector.empty)(_ ++ _)
//         assertEquals(merged, data)
//       }
//     }
//   }

//   test("Must fail when receives header before starting") {
//     forAllF(incomingContent) { content =>
//       for {
//         sut <- newChannel()
//         _ <- sut.cc
//           .recv(content.header)
//           .assertEquals(ReplyCode.UnexpectedFrame)
//       } yield ()
//     }
//   }

//   test("Must fail when receives header before content") {
//     forAllF(anyMethod, incomingContent.suchThat(!_.bodies.isEmpty)) {
//       (m, content) =>
//         for {
//           sut <- newChannel()
//           _ <- m match {
//             case m: ContentMethod    => sut.cc.asyncNotify(m)
//             case m: BasicClass.GetOk => sut.cc.get(m).void
//           }
//           _ <- sut.cc
//             .recv(content.bodies.head)
//             .assertEquals(ReplyCode.UnexpectedFrame)
//         } yield ()
//     }
//   }

//   test("Must recieve and merge all deliveries") {
//     forAllF(anyContents) { contents =>
//       for {
//         sut <- newChannel()
//         _ <- contents.traverse {
//           case (m: ContentMethod, content) =>
//             assertAsyncContent(m, content, sut.cc)
//           case (m: BasicClass.GetOk, content) =>
//             assertSyncContent(m, content, sut.cc)
//         }
//       } yield ()
//     }
//   }

//   private def assertAsyncContent(
//       method: ContentMethod,
//       content: IncomingContent,
//       cc: ContentChannel[IO]
//   ) = for {
//     _ <- cc.consume.size.assertEquals(0)
//     _ <- cc.asyncNotify(method)
//     _ <- cc.recv(content.header)
//     _ <- content.bodies.traverse(cc.recv)
//     _ <- cc.consume.size.assertEquals(1)
//     msg = Message(content.payload, content.properties)
//     expected = method match {
//       case m: BasicClass.Deliver =>
//         DeliveredMessage(
//           consumerTag = m.consumerTag,
//           deliveryTag = m.deliveryTag,
//           redelivered = m.redelivered,
//           exchange = m.exchange,
//           routingKey = m.routingKey,
//           msg
//         )
//       case m: BasicClass.Return =>
//         ReturnedMessage(
//           replyCode = m.replyCode,
//           replyText = m.replyText,
//           exchange = m.exchange,
//           routingKey = m.routingKey,
//           msg
//         )
//     }
//     _ <- cc.consume.take.assertEquals(expected)
//   } yield ()
//   private def assertSyncContent(
//       m: BasicClass.GetOk,
//       content: IncomingContent,
//       cc: ContentChannel[IO]
//   ) = for {
//     msgDef <- cc.get(m)
//     _ <- msgDef.tryGet.assertEquals(None)
//     _ <- cc.recv(content.header)
//     _ <- content.bodies.traverse(cc.recv)
//     _ <- msgDef.tryGet.assertEquals(
//       SynchronousGet(
//         deliveryTag = m.deliveryTag,
//         redelivered = m.redelivered,
//         exchange = m.exchange,
//         routingKey = m.routingKey,
//         messageCount = m.messageCount,
//         message = content.message
//       ).some
//     )
//   } yield ()
// }

// object ContentChannelSuite {

//   final case class SUT(
//       pq: Queue[IO, Frame],
//       cc: ContentChannel[IO]
//   )

//   private def newChannel(
//       ch: ChannelNumber = ChannelNumber(1),
//       maxFrameSize: Long = 10L,
//       size: Int = 0
//   ) = for {
//     pq <- Queue.bounded[IO, Frame](size)
//     s <- SequentialOutput(pq)
//     cc <- ContentChannel[IO](
//       ch,
//       maxSize = maxFrameSize,
//       publisher = s
//     )
//   } yield SUT(pq, cc)

//   val channel = DomainGenerators.channelNumber
//   val binary = Gen
//     .choose(0, 1000)
//     .flatMap(n =>
//       Gen
//         .containerOfN[Array, Byte](n, Arbitrary.arbitrary[Byte])
//         .map(ByteVector(_))
//     )
//   val maxSize = Gen.choose[Long](5, 100)
//   val props = DomainGenerators.properties

//   final case class IncomingContent(
//       header: Frame.Header,
//       bodies: List[Frame.Body]
//   ) {
//     def payload: ByteVector =
//       bodies.map(_.payload).foldLeft(ByteVector.empty)(_ ++ _)
//     def properties: Properties = header.props

//     def message: Message = Message(payload, properties)
//   }

//   val incomingContent: Gen[IncomingContent] = for {
//     ch <- channel
//     p <- props
//     ms <- maxSize
//     data <- binary
//   } yield IncomingContent(
//     header = Frame.Header(ch, ClassId(10), data.size, p),
//     bodies = List
//       .range(0L, data.size, ms)
//       .map(i => Frame.Body(ch, data.slice(i, i + ms)))
//   )

//   val contentMethods: Gen[ContentMethod] =
//     Gen.oneOf(BasicDataGenerator.deliverGen, BasicDataGenerator.returnGen)

//   val getMethod = BasicDataGenerator.getOkGen

//   val anyMethod: Gen[ContentMethod | BasicClass.GetOk] =
//     Gen.oneOf(contentMethods, getMethod)

//   val syncContent: Gen[(BasicClass.GetOk, IncomingContent)] = for {
//     m <- getMethod
//     c <- incomingContent
//   } yield (m, c)

//   val asyncContent: Gen[(ContentMethod, IncomingContent)] = for {
//     m <- contentMethods
//     c <- incomingContent
//   } yield (m, c)

//   type AnyMethod = ContentMethod | BasicClass.GetOk
//   val anyContent: Gen[(AnyMethod, IncomingContent)] = for {
//     m <- Gen.oneOf[AnyMethod](contentMethods, getMethod)
//     c <- incomingContent
//   } yield (m, c)

//   val anyContents: Gen[List[(AnyMethod, IncomingContent)]] = for {
//     n <- Gen.choose(1, 5)
//     cs <- Gen.listOfN(n, anyContent)
//   } yield cs
// }
