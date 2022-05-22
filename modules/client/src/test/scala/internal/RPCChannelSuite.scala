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
import cats.effect.std.Queue
import cats.implicits.*
import lepus.codecs.AllClassesDataGenerator.methods
import lepus.codecs.DomainGenerators.channelNumber
import lepus.protocol.Frame
import lepus.protocol.constants.ReplyCode
import munit.CatsEffectSuite
import munit.ScalaCheckEffectSuite
import org.scalacheck.Gen
import org.scalacheck.effect.PropF.forAllF

import RPCChannelSuite.*

class RPCChannelSuite extends CatsEffectSuite, ScalaCheckEffectSuite {
  test("send no wait") {
    forAllF(methods, channelNumber) { (m, ch) =>
      for {
        q <- Queue.bounded[IO, Frame](1)
        rpc <- RPCChannel(q, ch)
        _ <- q.size.assertEquals(0)
        _ <- rpc.sendNoWait(m)
        _ <- q.size.assertEquals(1)
        _ <- q.take.assertEquals(Frame.Method(ch, m))
      } yield ()

    }
  }

  test("send wait") {
    forAllF(methods, methods, channelNumber) { (m1, m2, ch) =>
      for {
        q <- Queue.bounded[IO, Frame](1)
        rpc <- RPCChannel(q, ch)
        _ <- q.size.assertEquals(0)
        _ <- rpc
          .sendWait(m1)
          .both(q.take.assertEquals(Frame.Method(ch, m1)) >> rpc.recv(m2))
          .map(_._1)
          .assertEquals(m2)
      } yield ()

    }
  }

  test("send wait ordering") {
    forAllF(methodPairs, channelNumber) { (mps, ch) =>
      for {
        q <- Queue.bounded[IO, Frame](mps.size)
        rpc <- RPCChannel(q, ch)
        _ <- q.size.assertEquals(0)
        pairs = mps.toList.unzip
        requests = pairs._1
        responses = pairs._2
        out <- requests
          .parTraverse(rpc.sendWait)
          .both(
            responses.traverse(resp =>
              q.take.flatMap {
                case Frame.Method(chNum, m) if chNum == ch =>
                  IO(m, resp) <* rpc.recv(resp)
                case other => fail(s"Invalid frame sent! $other")
              }
            )
          )
      } yield {
        val received = out._1
        val expectedMapping = out._2.toMap
        val mapping = requests.zip(received).toMap

        assertEquals(mapping, expectedMapping)
      }
    }
  }

  test("fails on recv when no one is waiting") {
    forAllF(methods, channelNumber) { (m, ch) =>
      for {
        q <- Queue.bounded[IO, Frame](1)
        rpc <- RPCChannel(q, ch)
        _ <- rpc.recv(m).assertEquals(ReplyCode.SyntaxError)
        _ <- q.size.assertEquals(0)
      } yield ()

    }
  }
}

object RPCChannelSuite {
  private val methodPair = for {
    m1 <- methods
    m2 <- methods
  } yield (m1, m2)

  private val methodPairs = for {
    n <- Gen.choose(2, 20)
    m <- Gen.mapOfN(n, methodPair)
  } yield m
}
