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

import lepus.wire.DomainCodecs
import scodec.*
import scodec.codecs.*

class ReverseByteAlignedSuite extends CodecTest {
  extension [T](self: Codec[T]) {
    def encodeBin(t: T): Option[String] = self.encode(t).toOption.map(_.toBin)
  }

  test("1 bit") {
    val codec = DomainCodecs.reverseByteAligned(bool)

    assertEquals(codec.encodeBin(true), Some("00000001"))
    assertEquals(codec.encodeBin(false), Some("00000000"))
  }

  test("2 bit") {
    val codec = DomainCodecs.reverseByteAligned(bool :: bool)

    assertEquals(codec.encodeBin((true, true)), Some("00000011"))

    assertEquals(codec.encodeBin((false, true)), Some("00000010"))
  }
}
