package lepus.protocol.gen

import fs2.Stream
import cats.effect.IO
import scala.xml.NodeSeq
import fs2.Pipe
import fs2.io.file.Path

def header(cls: Class) = Stream(
  "package lepus.protocol.classes." + cls.name.toLowerCase,
  "\n",
  "import lepus.protocol.Method",
  "import lepus.protocol.domains.*",
  "import lepus.protocol.constants.*",
  "\n"
)

def requestsBody(cls: Class): Stream[IO, String] =
  (Stream(
    s"enum Requests(classId: ClassId, methodId: MethodId) extends Method(classId, methodId) {"
  ) ++
    Stream
      .emits(cls.methods)
      .filter(_.receiver != MethodReceiver.Client)
      .map(m =>
        methodCodeGen(
          m
        ) + s" extends Requests(ClassId(${cls.id}), MethodId(${m.id}))"
      ) ++
    Stream("}"))
    .intersperse("\n")

def requests(cls: Class): Stream[IO, Nothing] =
  (header(cls) ++ requestsBody(cls))
    .through(
      generate(Path(s"classes/${cls.name.toLowerCase}/Requests.scala"))
    )

def responsesBody(cls: Class): Stream[IO, String] =
  (Stream(
    s"enum Responses(classId: ClassId, methodId: MethodId) extends Method(classId, methodId) {"
  ) ++
    Stream
      .emits(cls.methods)
      .filter(_.receiver != MethodReceiver.Server)
      .map(m =>
        methodCodeGen(
          m
        ) + s" extends Responses(ClassId(${cls.id}), MethodId(${m.id}))"
      ) ++
    Stream("}"))
    .intersperse("\n")

def responses(cls: Class): Stream[IO, Nothing] =
  (header(cls) ++ responsesBody(cls)).through(
    generate(Path(s"classes/${cls.name.toLowerCase}/Responses.scala"))
  )

def classCodeGen: Pipe[IO, Class, Nothing] = _.map { cls =>
  requests(cls) merge responses(cls)
}.parJoinUnbounded

private def methodCodeGen(method: Method): String =
  val fields =
    if method.fields.isEmpty then ""
    else
      "(" + method.fields
        .filterNot(_.dataType.isBlank)
        .map(fieldCodeGen)
        .mkString(",\n") + ")"
  val caseName = idName(method.name)
  s"""  case $caseName$fields"""

private def fieldCodeGen(field: Field): String =
  val name = field.name match {
    case "type" => "`type`"
    case other  => varName(other)
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

def genClasses(protocol: NodeSeq): Stream[IO, Nothing] =
  Stream.emits(buildClassModels(protocol)).through(classCodeGen)
