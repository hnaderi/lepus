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

package lepus.std

import cats.syntax.all.*
import munit.FunSuite

class TopicNameEncoderSuite extends FunSuite {
  test("from string") {
    val enc = TopicNameEncoder.from[String](identity)
    assertEquals(enc.get("hello"), TopicName("hello").asRight)
    assert(enc.get("invalid#string").isLeft)
  }

  test("sum type") {
    val enc = TopicNameEncoder.of[ADT]
    assertEquals(enc.get(ADT.A), Right(TopicName("A")))
    assertEquals(enc.get(ADT.B(1)), Right(TopicName("B")))
    assertEquals(enc.get(ADT.C("", 0)), Right(TopicName("C")))
  }

  test("product type") {
    val enc = TopicNameEncoder.of[ProductType]
    assertEquals(enc.get(ProductType(1, "")), Right(TopicName("ProductType")))
  }

  test("invalid type name") {
    assert(compileErrors("""TopicNameEncoder.of[`Invalid*Name`]""").nonEmpty)
  }

}

private enum ADT {
  case A
  case B(i: Int)
  case C(s: String, l: Long)
}

private final case class ProductType(i: Int, s: String)

private final case class `Invalid*Name`()
