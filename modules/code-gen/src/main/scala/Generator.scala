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

import cats.effect.*
import cats.implicits.*
import fs2.Pipe
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.file.writeAll
import fs2.text.utf8

import scala.xml.NodeSeq
import scala.xml.*

def gen: IO[Unit] = for {
  protocol <- IO(XML.load("amqp0-9-1.extended.xml"))
  classes = Extractors.classes(protocol)
  generation = Stream(
    APIDefs.generate(classes),
    ClassDefs.generate(classes),
    ClassCodecs.generate(classes),
    MethodCodecs.generate(classes),
    ClassDataGenerators.generate(classes)
  ).parJoinUnbounded
  _ <- generation.compile.drain
} yield ()

object Generator extends IOApp {
  def run(args: List[String]): IO[ExitCode] = gen.as(ExitCode.Success)
}
