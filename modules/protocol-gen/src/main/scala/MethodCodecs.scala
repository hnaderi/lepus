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
import fs2.io.file.Path

import scala.annotation.tailrec
import scala.xml.NodeSeq

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
        s".subcaseP[${tpeName(m)}](MethodId(${m.id})){case m:${tpeName(m)}=> m}(${valName(m.name)}Codec)"
      ) ++ Stream(s""".withContext("${cls.name} methods")""")

  private def tpeName(m: Method): String = if (
    m.fields.filterNot(_.reserved).isEmpty
  ) then idName(m.name) + ".type"
  else idName(m.name)

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

  private final case class FoldState(
      code: String = "",
      aligning: Boolean = false
  )

  private val bitTypes =
    List("bit", "no-wait", "no-local", "no-ack", "redelivered")
  private def isBit(f: Field) = bitTypes.contains(f.dataType)

  //Generates codec for method data model,
  //Considering bits packing
  private def codecsFor(fields: Seq[Field]): String = {
    val bitFields = fields.takeWhile(isBit)
    if !bitFields.isEmpty then
      val otherFields = fields.dropWhile(isBit)
      val op = if bitFields.size == 1 then "::" else ":+"
      val bitsSection = bitFields.map(codecFor).mkString(" :: ")
      val padSize = 8 - bitFields.size % 8
      val aligned = s"reverseByteAligned($bitsSection)"
      if otherFields.isEmpty then s"($aligned)"
      else s"($aligned $op ${codecsFor(otherFields)})"
    else
      val fs = fields.takeWhile(!isBit(_))
      val otherFields = fields.dropWhile(!isBit(_))
      val section = fs.map(codecFor).mkString(" :: ")
      if otherFields.isEmpty then s"($section)"
      else if otherFields.size == 1 || fs.size == 1 then
        s"($section :: ${codecsFor(otherFields)})"
      else s"(($section) ++ ${codecsFor(otherFields)})"
  }

  private def bitCodecsFor(bitFields: Seq[Field]): String =
    val codec = bitFields.map(codecFor).mkString(" :: ")
    s"byteAligned($codec)"

  private def codecFor(field: Field): String =
    field.dataType match {
      case "bit" if field.reserved      => "bool.unit(false)"
      case "bit"                        => "bool"
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
      srcFile("client", Path(s"codecs/${cls.name.toLowerCase}.scala"))
    )

  def generate(clss: Seq[Class]): Stream[IO, Nothing] =
    Stream.emits(clss).map(generateMethodCodecs).parJoinUnbounded
}
