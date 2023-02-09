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
import lepus.protocol.domains.LongString
import lepus.protocol.domains.ShortString
import munit.FunSuite

class AuthenticationConfigSuite extends FunSuite {
  val sasl1 = SaslMechanism(ShortString("sasl1"), IO(LongString("1")), IO(_))
  val sasl2 = SaslMechanism(ShortString("sasl2"), IO(LongString("2")), IO(_))

  val auth = AuthenticationConfig(sasl1, sasl2)

  test("Empty") {
    assertEquals(auth.get(), None)
  }

  test("First") {
    assertEquals(auth.get("sasl1"), Some(sasl1))
  }

  test("Second") {
    assertEquals(auth.get("sasl2"), Some(sasl2))
  }

  test("Non existing") {
    assertEquals(auth.get("sasl3"), None)
  }
}
