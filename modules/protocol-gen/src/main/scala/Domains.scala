package lepus.protocol.gen

import fs2.Stream
import cats.effect.IO
import scala.xml.NodeSeq
import fs2.Pipe
import fs2.io.file.Path

def domainCodeGen: Pipe[IO, Domain, Nothing] = domains =>
  (Stream("package lepus.client.gen") ++ domains.map { d =>
    val DTName = idName(d.name)

    s"""
${d.doc.map(comment).getOrElse("")}
opaque type $DTName <: String = String
    """
  })
    .through(generate(Path("gen-domains.scala")))

def genDomains(protocol: NodeSeq): Stream[IO, Nothing] =
  Stream.emits(buildDomainModels(protocol)).through(domainCodeGen)
