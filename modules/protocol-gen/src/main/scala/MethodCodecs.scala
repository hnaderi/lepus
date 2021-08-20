package lepus.protocol.gen

import fs2.Stream
import cats.effect.IO
import scala.xml.NodeSeq
import fs2.Pipe
import fs2.io.file.Path
import cats.implicits.*
import scala.annotation.tailrec

object MethodCodecs {
  private def header(cls: Class) = Stream(
    s"package lepus.client.codecs.${cls.name.toLowerCase}",
    "\n",
    "import lepus.protocol.Method",
    "import lepus.protocol.domains.*",
    "import lepus.protocol.classes.*",
    "import lepus.protocol.constants.*",
    "import lepus.client.codecs.DomainCodecs.*",
    "import scodec.{Codec, Encoder, Decoder}",
    "import scodec.codecs.*",
    "\n"
  )

  private def allCodecsIn(cls: Class): Stream[IO, String] =
    header(cls) ++
      Stream.emits(cls.methods).map(codecFor)

  private def op(f1: Field, f2: Field) =
    if f1.reserved then " ~> "
    else if f2.reserved then " ~> "
    else " :: "

  private def op(f1: Field) =
    if f1.reserved then " ~> "
    else " :: "

  extension (f1: Field) {
    def appendTo(str: String): String = str + op(f1) + codecFor(f1)
  }

  def cc2(
      method: Method
  ): String =
    val tpe = idName(method.name)
    method.fields match {
    case Nil    => s"provide($tpe)"
    case nonEmpty =>
      val codec = nonEmpty.map(codecFor).mkString(" :: ")
      s"($codec).as[$tpe]"
  }

  private def codecFor(method: Method): String =
    val name = valName(method.name)
    val cType = idName(method.name)
    val fieldCodecs = method.fields.map(codecFor).zip(method.fields).toList
    val codec = cc2(method)
    s"""val ${name}Codec : Codec[$cType] = $codec.withContext("$name method")"""

  private def codecFor(field: Field): String =
    field.dataType match {
      case "bit"       => "bool"
      case "octet"     => "int8"
      case "short"     => "int16"
      case "long"      => "int32"
      case "longlong"  => "long(64)"
      case "shortstr"  => "shortString"
      case "longstr"   => "longString"
      case "timestamp" => "timestamp"
      case "table"     => "fieldTable"
      case other       => valName(other)
    }

  def generateMethodCodecs(cls: Class): Stream[IO, Nothing] =
    allCodecsIn(cls).through(
      generate("client", Path(s"codecs/${cls.name.toLowerCase}.scala"))
    )

  def generateAll(clss: Seq[Class]): Stream[IO, Nothing] =
    Stream.emits(clss).map(generateMethodCodecs).parJoinUnbounded
}
