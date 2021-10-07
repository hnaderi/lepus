package lepus.codecs

import scodec.bits.*
import scodec.codecs
import scodec.codecs.*
import com.rabbitmq.client.impl.AMQImpl
import com.rabbitmq.client.impl.MethodArgumentWriter
import com.rabbitmq.client.impl.ValueWriter
import java.io.DataOutputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import scala.collection.JavaConverters.*
import com.rabbitmq.client.impl.Frame
import lepus.protocol.domains.*
import java.util.{Map => JMap}

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
