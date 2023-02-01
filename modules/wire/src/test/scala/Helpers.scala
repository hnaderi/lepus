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

import cats.implicits.*
import lepus.protocol.domains.*
import munit.Location
import scodec.Attempt
import scodec.DecodeResult
import scodec.bits.*
import scodec.codecs
import scodec.codecs.*

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.{Map => JMap}

def assertReversed[T](
    original: T,
    obtained: Attempt[DecodeResult[T]]
)(using Location): Unit =
  obtained.toEither
    .map(v => munit.Assertions.assertEquals(original, v.value))
    .leftMap(e => munit.Assertions.fail(e.messageWithContext))
    .merge
