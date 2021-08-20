package lepus.protocol.gen

import fs2.Stream
import cats.effect.IO
import scala.xml.NodeSeq
import fs2.Pipe
import fs2.io.file.Path
import cats.implicits.*
import Helpers.*

object MethodCodecs {
  private def header(cls: Class) = headers(
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

  private def allCodecsIn(cls: Class): Lines =
    header(cls) ++ obj(cls.name + "Codecs") {
      Stream
        .emits(cls.methods)
        .map(codecFor) ++ discriminated(cls)
    }

  private def discriminated(cls: Class): Lines =
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
    val allFields = method.fields
    val avFields = allFields.filterNot(_.reserved)
    val cType =
      idName(method.name) + (if avFields.isEmpty then ".type" else "")

    val codec =
      if allFields.isEmpty then s"provide($tpe)"
      else if avFields.isEmpty then codecsFor(allFields) + s" ~> provide($tpe)"
      else codecsFor(allFields) + s".as[$cType]"

    s"""private val ${name}Codec : Codec[$cType] =
           $codec
             .withContext("$name method")"""

  private def codecsFor(fields: Seq[Field]): String =
    val codec = fields.map(codecFor).mkString(" :: ")
    s"($codec)"

  private def codecFor(field: Field): String =
    field.dataType match {
      case "bit" if field.reserved      => "bool(8).unit(false)"
      case "bit"                        => "bool(8)"
      case "octet"                      => "byte"
      case "short" if field.reserved    => "short16.unit(0)"
      case "short"                      => "short16"
      case "long"                       => "int32"
      case "longlong"                   => "long(64)"
      case "shortstr" if field.reserved => "emptyShortString"
      case "shortstr"                   => "shortString"
      case "longstr" if field.reserved  => "emptyLongString"
      case "longstr"                    => "longString"
      case "timestamp"                  => "timestamp"
      case "table"                      => "fieldTable"
      case other                        => valName(other)
    }

  private def generateMethodCodecs(cls: Class): Stream[IO, Nothing] =
    allCodecsIn(cls).through(
      file("client", Path(s"codecs/${cls.name.toLowerCase}.scala"))
    )

  def generate(clss: Seq[Class]): Stream[IO, Nothing] =
    Stream.emits(clss).map(generateMethodCodecs).parJoinUnbounded
}
