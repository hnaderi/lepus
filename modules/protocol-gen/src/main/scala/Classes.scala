package lepus.protocol.gen

import fs2.Stream
import cats.effect.IO
import scala.xml.NodeSeq
import fs2.Pipe
import fs2.io.file.Path

def classCode(cls: Class): Stream[IO, String] =
  Stream(
    "package lepus.client.gen",
    "\n",
    "import lepus.protocol.domains.*",
    "import lepus.protocol.constants.*",
    "\n",
    s"enum ${idName(cls.name)} {"
  ) ++
    Stream.emits(cls.methods).map(methodCodeGen) ++
    Stream("}")

def classCodeGen: Pipe[IO, Class, Nothing] = _.map { cls =>
  classCode(cls)
    .through(generate(Path(s"methods/${idName(cls.name)}.scala")))
}.parJoinUnbounded

private def methodCodeGen(method: Method): String =
  s"""  case ${idName(method.name)}(${method.fields
    .map(fieldCodeGen)
    .mkString(",\n")})"""

private def fieldCodeGen(field: Field): String =
  s"""${varName(field.name)}: ${typeFor(field.dataType)}"""

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
  case ""          => "Unit"
  case other       => idName(other)
}

def genClasses(protocol: NodeSeq): Stream[IO, Nothing] =
  Stream.emits(buildClassModels(protocol)).through(classCodeGen)
