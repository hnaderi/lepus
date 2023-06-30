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

import cats.effect.IO
import cats.effect.std.CountDownLatch
import fs2.Chunk
import fs2.Stream
import fs2.Stream.*
import munit.CatsEffectSuite
import scodec.bits.ByteVector

class TransportSuite extends CatsEffectSuite {
  test("Transmission starts with sending protocol header") {
    for {
      out <- IO.ref(Chunk.empty[Byte])
      finished <- CountDownLatch[IO](1)
      _ <- Transport
        .build[IO](
          exec(finished.await),
          _.chunks.foreach(c => out.update(_ ++ c)).onFinalize(finished.release)
        )
        .apply(empty)
        .compile
        .toList
        .assertEquals(Nil)
      _ <- out.get
        .map(_.toByteVector)
        .assertEquals(ByteVector('A', 'M', 'Q', 'P', 0, 0, 9, 1))
    } yield ()
  }
}
