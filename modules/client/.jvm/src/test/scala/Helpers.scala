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

package lepus.codecs

import com.rabbitmq.client.impl.MethodArgumentWriter
import com.rabbitmq.client.impl.ValueWriter
import lepus.protocol.domains.*
import scodec.bits.*

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.{Map => JMap}
import scala.jdk.CollectionConverters.*

final class JavaTroops {
  val os = ByteArrayOutputStream()
  val dos = DataOutputStream(os)
  val vw = ValueWriter(dos)
  val maw = MethodArgumentWriter(vw)

  def result = BitVector(os.toByteArray)
}

def writeMethod(f: com.rabbitmq.client.impl.Method): BitVector = {
  val troops = JavaTroops()
  import troops.*

  dos.writeShort(f.protocolClassId)
  dos.writeShort(f.protocolMethodId)
  f.writeArgumentsTo(maw)
  maw.flush()

  result
}

val toJavaDomain: FieldData => Any = {
  case l: Long => l
  case o       => o
}

def toJavaMap(m: Map[ShortString, FieldData]): JMap[String, Object] =
  m.foldLeft(Map.empty[String, Object]) { case (jmap, (k, v)) =>
    jmap.updated(k, toJavaDomain(v).asInstanceOf[Object])
  }.asJava

def writeMap(ft: FieldTable): BitVector = {
  val troops = JavaTroops()
  import troops.*

  vw.writeTable(toJavaMap(ft.values))
  vw.flush()

  result
}

def splitEvery(s: String, i: Int): String =
  s.grouped(i).map(_.mkString).mkString(",")
def bitSplit: String => String = splitEvery(_, 8)
