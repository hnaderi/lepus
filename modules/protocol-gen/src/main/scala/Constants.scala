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

package lepus.protocol.gen

import fs2.Stream
import cats.effect.IO
import fs2.io.file.Files
import fs2.io.file.Path
import cats.implicits.*
import scala.xml.NodeSeq
import Helpers.*

object Constants {
  def generate(protocol: NodeSeq): Stream[IO, Nothing] =
    Stream
      .emits(Extractors.constants(protocol))
      .map { case (name, value, cls) =>
        s"val ${idName(name)} : Short = $value // $cls"
      }
      .through(srcFile("protocol", Path("constants.scala")))
}
