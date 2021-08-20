package lepus.protocol.gen

import fs2.Stream
import fs2.io.file.writeAll
import fs2.text.utf8
import cats.effect.*
import fs2.io.file.Files
import fs2.io.file.Path
import cats.implicits.*
import scala.xml.NodeSeq
import fs2.Pipe
import scala.xml.*

def gen: IO[Unit] = for {
  protocol <- IO(XML.load("amqp0-9-1.extended.xml"))
  classes = Extractors.classes(protocol)
  generation = MethodCodecs.generateAll(classes) merge Classes.generate(classes)
  // generation = genClasses(protocol)
  //genConsts(    protocol  ) genDomains(protocol) merge genClasses(protocol)
  _ <- generation.compile.drain
} yield ()

object Generator extends IOApp {
  def run(args: List[String]): IO[ExitCode] = gen.as(ExitCode.Success)
}
