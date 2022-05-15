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
import cats.effect.std.Queue
import cats.implicits.*
import fs2.Chunk
import fs2.Stream
import fs2.Stream.*
import munit.CatsEffectSuite
import scodec.bits.ByteVector

import java.io.OutputStream

class TransportSuite extends CatsEffectSuite {
  test("Transmission starts with sending protocol header") {
    for {
      q <- Queue.unbounded[IO, Option[Chunk[Byte]]]
      _ <- Transport
        .build[IO](
          empty,
          _.chunks.evalMap(c => q.offer(c.some)).onFinalize(q.offer(None)).drain
        )
        .apply(empty)
        .compile
        .toList
        .assertEquals(Nil)
      _ <- fromQueueNoneTerminated(q).unchunks.compile
        .to(ByteVector)
        .assertEquals(ByteVector('A', 'M', 'Q', 'P', 0, 0, 9, 1))
    } yield ()
  }
}
