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

import scala.concurrent.duration.*

class OutputWriterSuite extends InternalTestSuite {
  test("Writes to underlying queue") {
    for {
      out <- InteractionList[Int]
      writer <- OutputWriter(out.add(_))
      _ <- writer.write(1)
      _ <- out.assert(1)
    } yield ()
  }

  test("Write fails after closing") {
    for {
      out <- InteractionList[Int]
      writer <- OutputWriter(out.add(_))
      _ <- writer.onClose
      _ <- writer.write(1).intercept[OutputWriter.ConnectionIsClosed.type]
      _ <- out.assert()
    } yield ()
  }

  check("Interrupts blocked writes when closes") {
    for {
      writer <- OutputWriter(_ => IO.never)
      _ <- IO
        .both(
          writer.onClose.delayBy(10.minutes),
          writer.write(1)
        )
        .intercept[OutputWriter.ConnectionIsClosed.type]
    } yield ()
  }
}
