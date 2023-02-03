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
import cats.effect.implicits.*
import cats.effect.std.Queue
import cats.effect.std.QueueSource
import cats.implicits.*
import lepus.codecs.FrameGenerators
import lepus.protocol.Frame
import org.scalacheck.Gen

import scala.concurrent.duration.*

class ChannelOutputSuite extends InternalTestSuite {

  test("Must write single frame") {
    forAllF { (f: Int) =>
      for {
        q <- Queue.unbounded[IO, Int]
        out <- ChannelOutput(q, 1)
        _ <- q.size.assertEquals(0)
        _ <- out.writeOne(f)
        _ <- q.size.assertEquals(1)
        _ <- q.take.assertEquals(f)
      } yield ()
    }
  }

  test("Must write all frames strictly sequentially") {
    forAllF { (is1: Vector[Int], is2: Vector[Int]) =>
      for {
        q <- Queue.unbounded[IO, Int]
        out <- ChannelOutput(q, 1)
        _ <- writeConcurrently(is1, is2, out)
        _ <- assertStrictlySequential(is1, is2, q)
      } yield ()
    }
  }

  test("Must write all frames strictly sequentially: concurrent unblock") {
    forAllF { (is1: Vector[Int], is2: Vector[Int]) =>
      for {
        q <- Queue.unbounded[IO, Int]
        out <- ChannelOutput(q, 1)
        _ <- writeConcurrently(is1, is2, out).both(out.unblock)
        _ <- assertStrictlySequential(is1, is2, q)
      } yield ()
    }
  }

  private def writeConcurrently(
      is1: Vector[Int],
      is2: Vector[Int],
      out: ChannelOutput[IO, Int]
  ) = is1.traverse(out.writeOne).both(out.writeAll(is2: _*))

  private def assertStrictlySequential(
      is1: Vector[Int],
      is2: Vector[Int],
      q: QueueSource[IO, Int]
  ) =
    val totalSize = is1.size + is2.size
    for {
      _ <- q.size.assertEquals(totalSize)
      all <- Vector.range(0, totalSize).traverse(_ => q.take)
    } yield {
      assertEquals(all.size, totalSize)
      val idx = all.indexOfSlice(is2)
      assertNotEquals(idx, -1)
      val sliceRemove =
        all.slice(0, idx) ++ all.slice(idx + is2.size, totalSize)
      assertEquals(sliceRemove.sorted, is1.sorted)
    }

  check("Must respect blocking: writeOne") {
    for {
      q <- Queue.unbounded[IO, Int]
      out <- ChannelOutput(q, 1)
      _ <- out.block
      _ <- out.writeOne(1).timeout(1000.days).attempt
      _ <- q.size.assertEquals(0)
      _ <- out.unblock
      _ <- out.writeOne(2)
      _ <- q.take.assertEquals(2)
    } yield ()
  }

  check("Must wait when blocked: writeOne") {
    for {
      q <- Queue.unbounded[IO, Int]
      out <- ChannelOutput(q, 1)
      _ <- out.block
      _ <- out
        .writeOne(1)
        .background
        .use(_ =>
          IO.sleep(1000.days) >>
            q.size.assertEquals(0) >>
            out.unblock >>
            q.take.assertEquals(1)
        )
    } yield ()
  }

  check("Must respect blocking: writeAll") {
    for {
      q <- Queue.unbounded[IO, Int]
      out <- ChannelOutput(q, 1)
      _ <- out.block
      _ <- out.writeAll(1, 2, 3).timeout(1000.days).attempt
      _ <- q.size.assertEquals(0)
      _ <- out.unblock
      _ <- out.writeAll(1, 2, 3)
      _ <- List.range(0, 3).traverse(_ => q.take).assertEquals(List(1, 2, 3))
    } yield ()
  }

  check("Must wait when blocked: writeAll") {
    for {
      q <- Queue.unbounded[IO, Int]
      out <- ChannelOutput(q, 1)
      _ <- out.block
      _ <- out
        .writeAll(1, 2, 3)
        .background
        .use(_ =>
          IO.sleep(1000.days) >>
            q.size.assertEquals(0) >>
            out.unblock >>
            List.range(0, 3).traverse(_ => q.take).assertEquals(List(1, 2, 3))
        )
    } yield ()
  }
}
