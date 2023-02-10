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

import java.util.concurrent.TimeoutException
import scala.concurrent.duration.*

class ChannelOutputSuite extends InternalTestSuite {

  test("Must write single frame") {
    forAllF { (f: Int) =>
      for {
        q <- FakeOutputWriterSink.of[Int]
        out <- ChannelOutput(q, 1)
        _ <- q.assertEmpty
        _ <- out.writeOne(f)
        _ <- q.assert(f)
      } yield ()
    }
  }

  test("Must write all frames strictly sequentially") {
    forAllF { (is1: List[Int], is2: List[Int]) =>
      for {
        q <- FakeOutputWriterSink.of[Int]
        out <- ChannelOutput(q, 1)
        _ <- writeConcurrently(is1, is2, out)
        _ <- assertStrictlySequential(is1, is2, q)
      } yield ()
    }
  }

  test("Must write all frames strictly sequentially: concurrent unblock") {
    forAllF { (is1: List[Int], is2: List[Int]) =>
      for {
        q <- FakeOutputWriterSink.of[Int]
        out <- ChannelOutput(q, 1)
        _ <- writeConcurrently(is1, is2, out).both(out.unblock)
        _ <- assertStrictlySequential(is1, is2, q)
      } yield ()
    }
  }

  private def writeConcurrently(
      is1: List[Int],
      is2: List[Int],
      out: ChannelOutput[IO, Int]
  ) = is1.traverse(out.writeOne).both(out.writeAll(is2: _*))

  private def assertStrictlySequential(
      is1: List[Int],
      is2: List[Int],
      q: FakeOutputWriterSink[Int]
  ) =
    val totalSize = is1.size + is2.size
    for {
      _ <- q.size.assertEquals(totalSize)
      all <- q.all
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
      q <- FakeOutputWriterSink.of[Int]
      out <- ChannelOutput(q, 1)
      _ <- out.block
      _ <- out.writeOne(1).timeout(1001.days).intercept[TimeoutException]
      _ <- q.assertEmpty
      _ <- out.unblock
      _ <- out.writeOne(2)
      _ <- q.assert(2)
    } yield ()
  }

  check("Must respect blocking: writeAll") {
    for {
      q <- FakeOutputWriterSink.of[Int]
      out <- ChannelOutput(q, 1)
      _ <- out.block
      _ <- out.writeAll(1, 2, 3).timeout(1001.days).intercept[TimeoutException]
      _ <- q.assertEmpty
      _ <- out.unblock
      _ <- out.writeAll(1, 2, 3)
      _ <- q.assert(1, 2, 3)
    } yield ()
  }
}
