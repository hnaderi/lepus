package lepus.protocol.gen

import fs2.Stream
import cats.effect.IO
import fs2.io.file.Files
import fs2.io.file.Path
import cats.implicits.*
import scala.xml.NodeSeq

def genConsts(protocol: NodeSeq): Stream[IO, Nothing] =
  Stream
    .emits(buildConstants(protocol))
    .map { case (name, value, cls) =>
      s"val ${idName(name)} : Short = $value // $cls"
    }
    .through(generate("protocol", Path("constants.scala")))
