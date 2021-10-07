package lepus.protocol.gen

import cats.effect.IO
import cats.implicits.*
import fs2.Pipe
import fs2.Stream
import fs2.Stream.*
import fs2.io.file.Path

import scala.xml.NodeSeq

import Helpers.*

object ClassDataGenerator {
  private val header = headers(
    "package lepus.codecs",
    "\n",
    "import lepus.protocol.classes.*",
    "import lepus.protocol.domains.*",
    "import lepus.protocol.constants.*",
    "import ArbitraryDomains.given",
    "import org.scalacheck.Gen",
    "import org.scalacheck.Arbitrary",
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
      if isSingleton then s"Gen.const($mthName)"
      else s"""for {
        ${fieldGen(mth.fields)}
      } yield $mthName(${argGen(mth.fields)}
        )
"""

    val arb =
      s"given Arbitrary[$tpeName] = Arbitrary($valName)"

    Stream(declare, define, arb)
  }

  private def fileNameFor(cls: Class) = idName(cls.name) + "DataGenerator"

  def generate(clss: Seq[Class]): Stream[IO, Nothing] =
    emits(clss).flatMap(cls =>
      dataGeneratorsFor(cls).through(
        testFile("client", Path(s"generators/${fileNameFor(cls)}.scala"))
      )
    )
}
