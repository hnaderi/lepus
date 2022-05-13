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
import lepus.protocol.domains.*
import org.scalacheck.Prop.*

import DomainGenerators.*

class DomainReversibility extends munit.ScalaCheckSuite {

  override def scalaCheckTestParameters =
    super.scalaCheckTestParameters
      .withMinSuccessfulTests(200)
      .withMaxDiscardRatio(10)

  property("Field tables") {
    forAll(fieldTable) { table =>
      val res = for {
        enc <- DomainCodecs.fieldTable.encode(table)
        dec <- DomainCodecs.fieldTable.decode(enc)
      } yield dec

      assertReversed(table, res)
    }
  }

  property("Short string") {
    forAll(shortString) { shs =>
      val res = for {
        enc <- DomainCodecs.shortString.encode(shs)
        dec <- DomainCodecs.shortString.decode(enc)
      } yield dec

      assertReversed(shs, res)
    }
  }

  property("Long string") {
    forAll(longString) { longStr =>
      val res = for {
        enc <- DomainCodecs.longString.encode(longStr)
        dec <- DomainCodecs.longString.decode(enc)
      } yield dec

      assertReversed(longStr, res)
    }
  }

  property("exchange name") {
    forAll(exchangeName) { exchName =>
      val res = for {
        enc <- DomainCodecs.exchangeName.encode(exchName)
        dec <- DomainCodecs.exchangeName.decode(enc)
      } yield dec

      assertReversed(exchName, res)
    }
  }

  property("queue name") {
    forAll(queueName) { qName =>
      val res = for {
        enc <- DomainCodecs.queueName.encode(qName)
        dec <- DomainCodecs.queueName.decode(enc)
      } yield dec

      assertReversed(qName, res)
    }
  }

  property("path") {
    forAll(path) { p =>
      val res = for {
        enc <- DomainCodecs.path.encode(p)
        dec <- DomainCodecs.path.decode(enc)
      } yield dec

      assertReversed(p, res)
    }
  }

  property("delivery mode") {
    forAll(deliveryMode) { d =>
      val res = for {
        enc <- DomainCodecs.deliveryMode.encode(d)
        dec <- DomainCodecs.deliveryMode.decode(enc)
      } yield dec

      assertReversed(d, res)
    }
  }

  property("timestamp") {
    forAll(timestamp) { t =>
      val res = for {
        enc <- DomainCodecs.timestamp.encode(t)
        dec <- DomainCodecs.timestamp.decode(enc)
      } yield dec

      assertReversed(t, res)
    }
  }

  property("field data") {
    forAll(fieldData) { fd =>
      val res = for {
        enc <- DomainCodecs.fieldData.encode(fd)
        dec <- DomainCodecs.fieldData.decode(enc)
      } yield dec

      assertReversed(fd, res)
    }
  }

  property("reply code") {
    forAll(replyCode) { rc =>
      val res = for {
        enc <- DomainCodecs.replyCode.encode(rc)
        dec <- DomainCodecs.replyCode.decode(enc)
      } yield dec

      assertReversed(rc, res)
    }
  }

}
