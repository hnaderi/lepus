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
import fs2.Pipe
import fs2.Stream
import fs2.io.file.Path

import Helpers.*

object APIDefs {
  private def header = headers(
    "package lepus.client",
    "package internal",
    "\n",
    "import cats.MonadError",
    "import cats.implicits.*",
    "import lepus.protocol.*",
    "import lepus.protocol.classes.*",
    "import lepus.protocol.constants.*",
    "import lepus.protocol.domains.*",
    "\n"
  )

  private def requestsBody(cls: Class): Stream[IO, String] =
    val tpe = idName(cls.name) + "API"
    (Stream(
      s"private[client] final class $tpe[F[_]](rpc: RPCChannel[F])(using F:MonadError[F, Throwable]) {"
    ) ++
      Stream
        .emits(cls.methods.filter(_.receiver.isRequest))
        .map(methodCodeGen(cls, _)) ++
      Stream("}"))
      .intersperse("\n")

  private def methodCodeGen(cls: Class, method: Method): String =
    val caseName = valName(method.name)
    val className = s"${idName(cls.name)}Class"
    val methodName = idName(method.name)

    val fields = method.args
    val fieldsStr =
      if fields.isEmpty then ""
      else "(" + fields.map(fieldCodeGen).mkString(",\n") + ")"
    val valuesStr =
      if fields.isEmpty then ""
      else "(" + fields.map(fieldName).mkString(", ") + ")"

    val respTypes = method.responses.map(t => cls.methods.find(_.name == t).get)
    val noWait = method.fields.exists(_.dataType == "no-wait")
    val returnType = respTypes
      .map(_.fullTypeName(cls))
      .reduceOption(_ + " | " + _)
      .getOrElse("Unit")

    val finalReturnType = if noWait then s"Option[$returnType]" else returnType

    val signature = s"  def $caseName$fieldsStr : F[$finalReturnType] = "

    val msg = s"$className.$methodName$valuesStr"

    val waitBody =
      if !respTypes.isEmpty then
        s"rpc.sendWait(msg).flatMap{ " +
          respTypes
            .map(s => s"case m: ${s.fullTypeName(cls)} => m.pure")
            .appended("case _=> F.raiseError(???)")
            .mkString("\n") + "}"
      else ""
    val noWaitBody = "rpc.sendNoWait(msg)"

    val body =
      if noWait then
        s"if noWait then $noWaitBody.as(None) else $waitBody.map(_.some)"
      else if returnType == "Unit" then noWaitBody
      else waitBody

    s"""$signature {
val msg = $msg
$body
}
"""

  private def fieldName(field: Field) = field.name match {
    case "type" => "`type`"
    case other  => valName(other)
  }
  private def fieldCodeGen(field: Field): String =
    s"""${fieldName(field)}: ${typeFor(field.dataType)}"""

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
    (header ++ Stream.emits(clss).flatMap(requestsBody))
      .through(
        srcFile("client", Path(s"APIs.scala"))
      )

  extension (self: Method) {
    def args: List[Field] = self.fields.filterNot(_.reserved)
    def needsArgs: Boolean = !self.args.isEmpty
    def constructName = idName(self.name)
    def typeName: String =
      val s = constructName
      if needsArgs then s else s"$s.type"
    def fullTypeName(cls: Class): String = s"${idName(cls.name)}Class.$typeName"
  }
}
