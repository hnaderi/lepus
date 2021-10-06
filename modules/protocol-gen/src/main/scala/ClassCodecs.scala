package lepus.protocol.gen

import fs2.Stream
import cats.effect.IO
import scala.xml.NodeSeq
import fs2.Pipe
import fs2.io.file.Path
import cats.implicits.*
import Helpers.*
import fs2.Stream.*

object ClassCodecs {
  private val header = headers(
    "package lepus.client.codecs",
    "\n",
    "import lepus.protocol.Method",
    "import lepus.protocol.domains.ClassId",
    s"import lepus.protocol.classes.*",
    "import lepus.client.codecs.DomainCodecs.classId",
    "import scodec.Codec",
    "import scodec.codecs.discriminated",
    "\n"
  )

  private def allCodecsIn(clss: Seq[Class]): Lines =
    header ++ obj("MethodCodec") {
      emit(
        "val all : Codec[Method] = discriminated[Method].by(classId)"
      ) ++ emits(clss)
        .flatMap(
          typecase
        ) ++
        emit(""".withContext("Method codecs")""")
    }

  private def typecase(cls: Class): Lines =
    emit(
      s""".subcaseP[${idName(
        cls.name
      )}Class](ClassId(${cls.id})){case m:${idName(
        cls.name
      )}Class=> m}(${idName(cls.name)}Codecs.all)"""
    )

  def generate(clss: Seq[Class]): Stream[IO, Nothing] =
    allCodecsIn(clss).through(
      file("client", Path(s"codecs/MethodCodec.scala"))
    )
}
