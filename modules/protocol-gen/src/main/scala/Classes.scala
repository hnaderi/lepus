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
import scala.xml.NodeSeq
import fs2.Pipe
import fs2.io.file.Path
import Helpers.*

object Classes {
  private def header(cls: Class) = headers(
    "package lepus.protocol.classes",
    "\n",
    "import lepus.protocol.*",
    "import lepus.protocol.domains.*",
    "import lepus.protocol.constants.*",
    "\n"
  )

  private def requestsBody(cls: Class): Stream[IO, String] =
    val tpe = idName(cls.name) + "Class"
    (Stream(
      s"enum $tpe(methodId: MethodId) extends Class(ClassId(${cls.id})) with Method(methodId) {"
    ) ++
      Stream
        .emits(cls.methods)
        .map(methodCodeGen(tpe, _)) ++
      Stream("}"))
      .intersperse("\n")

  private def requests(cls: Class): Stream[IO, Nothing] =
    (header(cls) ++ requestsBody(cls))
      .through(
        srcFile(
          "protocol",
          Path(s"classes/${cls.name.toLowerCase}/Methods.scala")
        )
      )

  private def classCodeGen: Pipe[IO, Class, Nothing] =
    _.map(requests).parJoinUnbounded

  private def methodCodeGen(superType: String, method: Method): String =
    val fields = method.fields.filterNot(_.reserved)
    val fieldsStr =
      if fields.isEmpty then ""
      else "(" + fields.map(fieldCodeGen).mkString(",\n") + ")"
    val caseName = idName(method.name)
    s"""  case $caseName$fieldsStr extends $superType(MethodId(${method.id})) ${sideFor(
      method
    )} """

  private def sideFor(method: Method): String = method.receiver match {
    case MethodReceiver.Server => "with Response"
    case MethodReceiver.Client => "with Request"
    case MethodReceiver.Both   => "with Response with Request"
  }

  private def fieldCodeGen(field: Field): String =
    val name = field.name match {
      case "type" => "`type`"
      case other  => valName(other)
    }
    s"""$name: ${typeFor(field.dataType)}"""

  private def typeFor(str: String): String = str match {
    case "bit"       => "Boolean"
    case "octet"     => "Byte"
    case "short"     => "Short"
    case "long"      => "Int"
    case "longlong"  => "Long"
    case "shortstr"  => "ShortString"
    case "longstr"   => "LongString"
    case "timestamp" => "Timestamp"
    case "table"     => "FieldTable"
    case other       => idName(other)
  }

  def generate(clss: Seq[Class]): Stream[IO, Nothing] =
    Stream.emits(clss).through(classCodeGen)

}
