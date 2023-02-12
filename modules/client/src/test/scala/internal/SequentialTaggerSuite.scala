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

package lepus.client.internal

import cats.effect.IO
import cats.syntax.all.*
import lepus.protocol.domains.DeliveryTag

class SequentialTaggerSuite extends InternalTestSuite {
  test("Starts at 1") {
    SequentialTagger[IO].flatMap(_.next(IO.unit)).assertEquals(1)
  }

  test("Increments") {
    SequentialTagger[IO]
      .flatMap(_.next(IO.unit).replicateA(3))
      .assertEquals(List(DeliveryTag(1), DeliveryTag(2), DeliveryTag(3)))
  }

  test("Ensures serializability of concurrent operations") {
    for {
      running <- IO.ref(0)
      results <- IO.ref(List.empty[Int])
      tagger <- SequentialTagger[IO]
      _ <- tagger
        .next(
          running.update(_ + 1) >> running
            .updateAndGet(_ - 1)
            .flatMap(r => results.update(r :: _))
        )
        .parReplicateA(10)
      _ <- results.get.assertEquals(List.fill(10)(0))
    } yield ()
  }
}
