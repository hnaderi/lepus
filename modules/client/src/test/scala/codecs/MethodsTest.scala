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

class MethodsTest extends CodecTest {

  override def scalaCheckTestParameters =
    super.scalaCheckTestParameters
      .withMinSuccessfulTests(2000)
      .withMaxDiscardRatio(10)

  property("All methods codecs must be reversible") {
    forAll(AllClassesDataGenerator.methods) { m =>
      val res = for {
        enc <- MethodCodec.all.encode(m)
        dec <- MethodCodec.all.decode(enc)
      } yield dec

      assertReversed(m, res)
    }
  }

}
