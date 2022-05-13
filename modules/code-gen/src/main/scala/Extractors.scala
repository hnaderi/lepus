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

import scala.xml.*

object Extractors {
  def constants(protocol: NodeSeq): Seq[(String, Short, String)] =
    (protocol \ "constant").map(c =>
      (c \@ "name", (c \@ "value").toShort, c \@ "class")
    )

  def domains(protocol: NodeSeq): Seq[Domain] = (protocol \ "domain")
    .filter(d => d \@ "name" != d \@ "type")
    .map { d =>
      val doc = (d \ "doc").text
      Domain(
        name = d \@ "name",
        dataType = PrimitiveType.valueOf(d \@ "type"),
        label = d \@ "label",
        doc = Option.when(!doc.trim.isEmpty)(doc),
        assertions = (d \ "assert").toList.map(_.text)
      )
    }

  def classes(protocol: NodeSeq): Seq[Class] =
    (protocol \ "class").map(c =>
      Class(
        name = c \@ "name",
        label = c \@ "label",
        id = (c \@ "index").toShort,
        doc = (c \ "doc").map(_.text).headOption.getOrElse(""),
        methods = buildMethodModels(c).toList
      )
    )

  private def buildMethodModels(thisClass: NodeSeq): Seq[Method] =
    (thisClass \ "method").map { c =>
      val sync =
        if (c \@ "synchronous") == "1" then MethodType.Sync
        else MethodType.ASync
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

  private def buildFieldModels(thisMethod: NodeSeq): Seq[Field] =
    (thisMethod \ "field").map { c =>
      val domain = c \@ "domain"
      val tpe = c \@ "type"
      val dataType = if domain.trim.isEmpty then tpe else domain
      Field(
        name = c \@ "name",
        label = c \@ "label",
        doc = (c \ "doc").map(_.text).headOption.getOrElse(""),
        dataType = dataType,
        reserved = c \@ "reserved" == "1"
      )
    }

}
