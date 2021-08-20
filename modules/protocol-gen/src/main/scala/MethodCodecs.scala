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
    "package lepus.client.codecs",
    "\n",
    "import lepus.protocol.*",
    "import lepus.protocol.domains.*",
    s"import lepus.protocol.classes.*",
    s"import lepus.protocol.classes.${idName(cls.name)}Class.*",
    "import lepus.protocol.constants.*",
    "import lepus.client.codecs.DomainCodecs.*",
    "import scodec.{Codec, Encoder, Decoder}",
    "import scodec.codecs.*",
    "\n"
  )

  private def obj(cls: Class)(s: Stream[IO, String]) =
    (Stream(s"object ${idName(cls.name)}Codecs {") ++ s ++ Stream("}"))
      .intersperse("\n")

  private def allCodecsIn(cls: Class): Stream[IO, String] =
    header(cls) ++ obj(cls) {
      Stream
        .emits(cls.methods)
        .map(codecFor) ++ discriminated(cls)
    }

  private def discriminated(cls: Class): Stream[IO, String] =
    Stream(
      s"val all : Codec[${idName(cls.name)}Class] =",
      s"discriminated[${idName(cls.name)}Class].by(methodId)"
    ) ++ Stream
      .emits(cls.methods)
      .map(m =>
        s".typecase(MethodId(${m.id}), ${valName(m.name)}Codec)"
      ) ++ Stream(s""".withContext("${cls.name} methods")""")

  private def codecFor(method: Method): String =
    val tpe = idName(method.name)
    val name = valName(method.name)
    val fields = method.fields.filterNot(_.reserved)
    val codec = fields match {
      case Nil => s"provide($tpe)"
      case nonEmpty =>
        val codec = nonEmpty.map(codecFor).mkString(" :: ")
        s"($codec).as[$tpe]"
    }
    val cType =
      idName(method.name) + (if fields.isEmpty then ".type" else "")
    s"""private val ${name}Codec : Codec[$cType] =
           $codec
             .withContext("$name method")"""

  private def codecFor(field: Field): String =
    field.dataType match {
      case "bit"       => "bool(8)"
      case "octet"     => "byte"
      case "short"     => "short16"
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
