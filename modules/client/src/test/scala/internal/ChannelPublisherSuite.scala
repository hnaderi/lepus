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
import cats.effect.std.Queue
import cats.implicits.*
import lepus.codecs.BasicDataGenerator
import lepus.codecs.DomainGenerators
import lepus.protocol.Frame
import lepus.protocol.domains.*
import munit.CatsEffectSuite
import munit.ScalaCheckEffectSuite
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.effect.PropF.*
import scodec.bits.ByteVector

import ChannelPublisherSuite.*

class ChannelPublisherSuite extends InternalTestSuite {

  test("Must split publish data to frames with maximum permitted size") {
    forAllNoShrinkF(
      BasicDataGenerator.publishGen,
      channel,
      maxSize,
      binary,
      props
    ) { (publishMethod, ch, size, data, props) =>
      val frameCount = Math.ceil(data.size.toDouble / size.toDouble).toInt
      for {
        pq <- Queue.unbounded[IO, Frame]
        sout <- ChannelOutput(pq, 1000)
        sut = ChannelPublisher[IO](ch, size, sout)

        _ <- sut.send(publishMethod, Message(data, props))

        _ <- pq.size.assertEquals(frameCount + 2)

        _ <- pq.take.assertEquals(Frame.Method(ch, publishMethod))
        _ <- pq.take.assertEquals(
          Frame.Header(
            ch,
            ClassId(10),
            bodySize = data.size,
            props
          )
        )
        _ <- pq.size.assertEquals(frameCount)

        all <- (1 to frameCount).toList.traverse(_ => pq.take)

        expected = Range
          .Long(0, data.size, size)
          .map(i => Frame.Body(ch, data.slice(i, i + size)))
          .toList

      } yield {
        assertEquals(all, expected)
        val merged = all
          .collect { case Frame.Body(_, pl) =>
            pl
          }
          .foldLeft(ByteVector.empty)(_ ++ _)
        assertEquals(merged, data)
      }
    }
  }
}

object ChannelPublisherSuite {
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
}
