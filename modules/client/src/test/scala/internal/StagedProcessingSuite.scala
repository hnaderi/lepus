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
import fs2.Stream
import fs2.Stream.*

import Helpers.*

class StagedProcessingSuite extends InternalTestSuite {
  test("Staged run") {
    val s = range(1, 10)
      .stagedRun(
        i => exec(IO.println(s"Doing somework with $i")),
        i => Stream(i + 100) ++ exec(IO.println(s"Passing $i"))
      )

    s.compile.toList.assertEquals(List.range(3, 10).prepended(102))
  }

  test("Puller") {
    val s = range[IO, Int](1, 10)

    val p = Puller.pull[IO, Int].eval(IO.println)
    def prog: Puller[IO, Int, Unit] = for {
      a <- p.eval(a => IO.println(s"First one is $a"))
      b <- p.eval(b => IO.println(s"Second one is $b"))
      _ <- if b < 5 then p.void else prog
    } yield ()

    s.run(p).compile.toList.assertEquals(List.range(2, 10)) >>
      s.run(prog).compile.toList.assertEquals(List.range(4, 10))
  }

}
