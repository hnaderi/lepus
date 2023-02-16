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

package test

import lepus.protocol.domains.ShortString
import lepus.std.*
import munit.FunSuite
import munit.Location
import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll
import org.scalacheck.Arbitrary
import java.util.UUID

class ShortStringHashConstructionSuite extends FunSuite, ScalaCheckSuite {
  private def check[T: Arbitrary](name: String, f: T => ShortString)(using
      Location
  ) =
    property(s"$name is valid") {
      forAll { (s: T) =>
        val hash = f(s)
        assertEquals(ShortString.from(hash), Right(hash))
      }
    }

  check("md5", ShortString.md5Hex(_))
  check("sha1", ShortString.sha1Hex(_))
  check("sha224", ShortString.sha224Hex(_))
  check("sha256", ShortString.sha256Hex(_))
  check("sha384", ShortString.sha384Hex(_))
  check("sha512", ShortString.sha512Hex(_))
}
