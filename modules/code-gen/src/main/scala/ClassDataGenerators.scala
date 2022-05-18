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

import scala.xml.NodeSeq

import Helpers.*

object ClassDataGenerators {
  private val header = headers(
    "package lepus.codecs",
    "\n",
    "import lepus.protocol.*",
    "import lepus.protocol.constants.*",
    "import lepus.protocol.domains.*",
    "import org.scalacheck.Arbitrary",
    "import org.scalacheck.Gen",
    "import ArbitraryDomains.given",
    "\n"
  )

  private def dataGeneratorsFor(cls: Class): Lines =
    header ++ obj(fileNameFor(cls))(
      emits(cls.methods).flatMap(dataGeneratorsFor(cls, _)) ++ classGen(cls)
    )

  private val genName: String => String = n => s"${valName(n)}Gen"

  private def classGen(cls: Class): Lines =
    val methodGens =
      cls.methods.map(_.name).map(genName).mkString(",")
    val clsName = s"${idName(cls.name)}Class"
    Stream(
      "\n",
      s" val classGen : Gen[$clsName] = Gen.oneOf($methodGens)",
      s" given Arbitrary[$clsName] = Arbitrary(classGen)"
    )

  private val dataFields: Seq[Field] => Seq[(Field, Int)] =
    _.filterNot(_.reserved).zipWithIndex

  private def typeOf: Field => String = _.dataType match {
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

  private def fieldGen(fs: Seq[Field]): String =
    dataFields(fs)
      .map((f, idx) => s"arg$idx <- Arbitrary.arbitrary[${typeOf(f)}]")
      .mkString("\n")

  private def argGen(fs: Seq[Field]): String =
    dataFields(fs).map((f, idx) => s"arg$idx").mkString(",")

  private def dataGeneratorsFor(cls: Class, mth: Method): Lines = {
    val isSingleton = mth.fields.forall(_.reserved)

    val mthName = s"${idName(cls.name)}Class.${idName(mth.name)}"
    val tpeName = if isSingleton then s"$mthName.type" else mthName

    val valName = genName(mth.name)

    val declare = s"val $valName : Gen[$tpeName] ="
    val define =
      if isSingleton then s"Gen.const($mthName)\n"
      else s"""for {
        ${fieldGen(mth.fields)}
      } yield $mthName(${argGen(mth.fields)}
        )

"""

    val arb =
      s"given Arbitrary[$tpeName] = Arbitrary($valName)\n"

    Stream(declare, define, arb)
  }

  private def fileNameFor(cls: Class) = idName(cls.name) + "DataGenerator"

  private def allDataGen(clss: Seq[Class]): Lines =
    header ++ obj("AllClassesDataGenerator")(
      emit(
        "val methods : Gen[Method] = Gen.oneOf(" ++ clss
          .map(cls => fileNameFor(cls) + ".classGen")
          .mkString(",") + ")"
      )
    )

  def generate(clss: Seq[Class]): Stream[IO, Nothing] =
    emits(clss).flatMap(cls =>
      dataGeneratorsFor(cls).through(
        srcFile(
          "protocol-testkit",
          Path(s"generators/${fileNameFor(cls)}.scala")
        )
      )
    ) merge allDataGen(clss).through(
      srcFile(
        "protocol-testkit",
        Path(s"generators/AllClassesDataGenerator.scala")
      )
    )
}
