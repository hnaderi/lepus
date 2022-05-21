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
import lepus.protocol.Frame
import lepus.protocol.classes.basic.Properties
import lepus.protocol.domains.*
import munit.CatsEffectSuite
import munit.ScalaCheckSuite
import org.scalacheck.effect.PropF.forAllF
import scodec.bits.ByteVector

import scala.concurrent.duration.*

class ContentChannelSuite extends CatsEffectSuite, ScalaCheckSuite {
  override def munitTimeout = 1.second
  test("Sanity") {
    for {
      q <- Queue.bounded[IO, Frame](10)
      cc <- ContentChannel[IO](
        channelNumber = ChannelNumber(3),
        maxSize = 10,
        q
      )
      _ <- cc.send(Message(ByteVector(1, 2, 3)))
      _ <- q.take.assertEquals(
        Frame.Header(ChannelNumber(3), ClassId(10), bodySize = 3, Properties())
      )
      _ <- q.take.assertEquals(
        Frame.Body(ChannelNumber(3), ByteVector(1, 2, 3))
      )
      a = Array(1).toIndexedSeq
    } yield ()
  }
}

object ContentChannelSuite {}
