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

import lepus.wire.FrameCodec
import org.scalacheck.Prop.*

class FramesTest extends CodecTest {

  override def scalaCheckTestParameters =
    super.scalaCheckTestParameters
      .withMinSuccessfulTests(2000)
      .withMaxDiscardRatio(10)

  property("All frame codecs must be reversible") {
    forAll(FrameGenerators.frames) { f =>
      val res = for {
        enc <- FrameCodec.frame.encode(f)
        dec <- FrameCodec.frame.decode(enc)
      } yield dec

      assertReversed(f, res)
    }
  }

}
