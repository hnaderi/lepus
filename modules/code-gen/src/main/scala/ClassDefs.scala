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
import fs2.Stream
import fs2.io.file.Path

import Helpers.*

object ClassDefs {
  private def header = headers(
    "package lepus.protocol",
    "\n",
    "import lepus.protocol.*",
    "import lepus.protocol.constants.*",
    "import lepus.protocol.domains.*",
    "\n",
    """
enum Peer {
  case Server, Client, Both
}

enum MethodIntent {
  case Request, Response
}

sealed abstract class Method {
  val _classId: ClassId
  val _methodId: MethodId
  val _synchronous: Boolean
  val _receiver: Peer
  val _intent : MethodIntent
}

object Metadata {
  sealed trait Async extends Method {
    override val _synchronous = false
  }
  sealed trait Sync extends Method {
    override val _synchronous = true
  }
  sealed trait ServerMethod extends Method {
    override val _receiver = Peer.Server
  }
  sealed trait ClientMethod extends Method {
    override val _receiver = Peer.Client
  }
  sealed trait DualMethod extends Method {
    override val _receiver = Peer.Both
  }
  sealed trait Request extends Method {
    override val _intent = MethodIntent.Request
  }
  sealed trait Response extends Method {
    override val _intent = MethodIntent.Response
  }
}

import Metadata.*

"""
  )

  private def methodSupertype(cls: Class) = idName(cls.name) + "Class"

  private def classCodeGen(cls: Class): Stream[IO, String] =
    val tpe = methodSupertype(cls)
    val clazz = s"""
sealed trait $tpe extends Method {
  override val _classId = ClassId(${cls.id})
}

object $tpe {
"""
    val methods = cls.methods.map(methodCodeGen(cls, _))

    Stream.emits(methods.prepended(clazz).appended("}"))

  private def methodCodeGen(cls: Class, method: Method): String =
    val fields = method.fields.filterNot(_.reserved)
    val caseName = idName(method.name)
    val body =
      if fields.isEmpty then s"case object $caseName"
      else
        s"final case class $caseName(" + fields
          .map(fieldCodeGen)
          .mkString(",\n") + ")"

    val extendsType = List(
      methodSupertype(cls),
      if method.sync == MethodType.Sync then "Sync" else "Async",
      method.receiver match {
        case MethodReceiver.Client => "ServerMethod"
        case MethodReceiver.Server => "ClientMethod"
        case MethodReceiver.Both   => "DualMethod"
      },
      if cls.methods.exists(_.responses.contains(method.name)) then "Response"
      else "Request"
    ).mkString(" with ")

    s"""
$body extends $extendsType {
  override val _methodId = MethodId(${method.id})
}"""

  private def fieldCodeGen(field: Field): String =
    val name = field.name match {
      case "type" => "`type`"
      case other  => valName(other)
    }
    s"""$name: ${typeFor(field.dataType)}"""

  private def typeFor(str: String): String = str match {
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

  def generate(clss: Seq[Class]): Stream[IO, Nothing] =
    (header ++ Stream.emits(clss).flatMap(classCodeGen))
      .through(
        srcFile("protocol", Path(s"Classes.scala"))
      )

}
