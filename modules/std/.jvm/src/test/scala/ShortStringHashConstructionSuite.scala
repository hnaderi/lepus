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
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll

class ShortStringHashConstructionSuite extends FunSuite, ScalaCheckSuite {
  private def check(name: String, f: String => ShortString)(using
      Location
  ) =
    property(s"$name is valid") {
      forAll { (s: String) =>
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

  check("string ops md5", _.md5Hex)
  check("string ops sha1", _.sha1Hex)
  check("string ops sha224", _.sha224Hex)
  check("string ops sha256", _.sha256Hex)
  check("string ops sha384", _.sha384Hex)
  check("string ops sha512", _.sha512Hex)
}
