package lepus.protocol.gen

import fs2.Stream
import cats.effect.IO
import scala.xml.NodeSeq
import fs2.Pipe
import fs2.io.file.Path

def classCode(cls: Class): Stream[IO, String] =
  Stream("package lepus.client.gen", s"enum ${idName(cls.name)} {") ++
    Stream.emits(cls.methods).map(m => s"  case ${idName(m.name)}") ++
    Stream("}")

def classCodeGen: Pipe[IO, Class, Nothing] = _.map { cls =>
  classCode(cls)
    .through(generate(Path(s"methods/${idName(cls.name)}.scala")))
}.parJoinUnbounded
// def methodCodeGen[F[_]]: Pipe[F, Method, String] = ???

def genClasses(protocol: NodeSeq): Stream[IO, Nothing] =
  Stream.emits(buildClassModels(protocol)).through(classCodeGen)
