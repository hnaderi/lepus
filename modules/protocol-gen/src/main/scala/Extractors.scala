package lepus.protocol.gen

import scala.xml.*

def buildConstants(protocol: NodeSeq): Seq[(String, Short, String)] =
  (protocol \ "constant").map(c =>
    (c \@ "name", (c \@ "value").toShort, c \@ "class")
  )

def buildDomainModels(protocol: NodeSeq): Seq[Domain] = (protocol \ "domain")
  .filter(d => d \@ "name" != d \@ "type")
  .map { d =>
    val doc = (d \ "doc").text
    Domain(
      name = d \@ "name",
      dataType = PrimitiveType.valueOf(d \@ "type"),
      label = d \@ "label",
      doc = Option.when(!doc.isBlank)(doc),
      assertions = (d \ "assert").toList.map(_.text)
    )
  }

def buildClassModels(protocol: NodeSeq): Seq[Class] =
  (protocol \ "class").map(c =>
    Class(
      name = c \@ "name",
      label = c \@ "label",
      id = (c \@ "index").toShort,
      doc = (c \ "doc").map(_.text).headOption.getOrElse(""),
      methods = buildMethodModels(c).toList
    )
  )

def buildMethodModels(thisClass: NodeSeq): Seq[Method] =
  (thisClass \ "method").map { c =>
    val sync =
      if (c \@ "synchronous") == "1" then MethodType.Sync else MethodType.ASync
    val chassis = (c \ "chassis").map(_ \@ "name")
    val recv = chassis.length match {
      case 1 if chassis.contains("server") => MethodReceiver.Server
      case 1 if chassis.contains("client") => MethodReceiver.Client
      case _                               => MethodReceiver.Both
    }

    Method(
      name = c \@ "name",
      label = c \@ "label",
      id = (c \@ "index").toShort,
      sync = sync,
      receiver = recv,
      doc = (c \ "doc").map(_.text).headOption.getOrElse(""),
      fields = buildFieldModels(c).toList
    )
  }

def buildFieldModels(thisMethod: NodeSeq): Seq[Field] =
  (thisMethod \ "field").map { c =>
    val domain = c \@ "domain"
    val tpe = c \@ "type"
    val dataType = if domain.isBlank then tpe else domain
    Field(
      name = c \@ "name",
      label = c \@ "label",
      doc = (c \ "doc").map(_.text).headOption.getOrElse(""),
      dataType = dataType,
      reserved = c \@ "reserved" == "1"
    )
  }
