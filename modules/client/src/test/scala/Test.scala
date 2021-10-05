package lepus.client.java

import munit.FunSuite
import scodec.{Codec, Encoder, Decoder}
import scodec.bits.*
import scodec.codecs
import scodec.codecs.*
import lepus.client.codecs.DomainCodecs

class Test extends FunSuite {
  test("Builds!") {
    val data = BitVector(
      32, -32, 0, 0, 0, 32, 7, 118, 101, 114, 115, 105, 111, 110, 108, 0, 0, 0,
      0, 0, 0, 0, 12, 5, 115, 101, 113, 110, 114, 108, 0, 0, 0, 0, 0, 0, 0, 34,
      6, 101, 118, 45, 49, 48, 48, 0, 0, 0, 0, 97, 92, 58, 115, 10, 79, 114,
      100, 101, 114, 69, 118, 101, 110, 116
    )

    println(data.toHex)
    println(
      DomainCodecs.flags.decode(data).map(_.value.zipWithIndex.filter(_._1))
    )
    val res = DomainCodecs.basicProps.decode(data)
    println(res)
    val out = res.getOrElse(???)
    val enc = DomainCodecs.basicProps.encode(out.value)
    assertEquals(enc.getOrElse(???).toHex, data.toHex) //FIXME
  }
}
