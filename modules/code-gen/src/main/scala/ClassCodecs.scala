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

import cats.effect.IO
import cats.implicits.*
import fs2.Pipe
import fs2.Stream
import fs2.Stream.*
import fs2.io.file.Path

import Helpers.*

object ClassCodecs {
  private val header = headers(
    "package lepus.wire",
    "\n",
    "import lepus.protocol.*",
    "import lepus.protocol.domains.ClassId",
    "import lepus.wire.DomainCodecs.classId",
    "import scodec.Codec",
    "import scala.annotation.switch",
    "\n"
  )

  private def allCodecsIn(clss: Seq[Class]): Lines =
    header ++ obj("MethodCodec") {
      emit(
        "val all : Codec[Method] = classId.flatZip(m=> ((m:Short): @switch) match {"
      ) ++ emits(clss)
        .flatMap(
          typecase
        ) ++
        emit(
          """}).xmap(_._2, a => (a._classId, a)).withContext("Method codecs")"""
        )
    }

  private def typecase(cls: Class): Lines =
    emit(
      s"""case ${cls.id} => ${idName(cls.name)}Codecs.all.upcast[Method]"""
    )

  def generate(clss: Seq[Class]): Stream[IO, Nothing] =
    allCodecsIn(clss).through(
      srcFile("wire", Path(s"codecs/MethodCodec.scala"))
    )
}
