/*
                           /!\ HUMANS BEWARE /!\
=================================================================================
||                                                                             ||
|| THIS FILE IS GENERATED BY MACHINES AND ANY EDITING BY HUMANS IS PROHIBITED! ||
||                                                                             ||
=================================================================================
 */

package lepus.client.codecs

import lepus.protocol.*
import lepus.protocol.domains.*
import lepus.protocol.classes.*
import lepus.protocol.classes.ConfirmClass.*
import lepus.protocol.constants.*
import lepus.client.codecs.DomainCodecs.*
import scodec.{Codec, Encoder, Decoder}
import scodec.codecs.*

object ConfirmCodecs {

  private val selectCodec: Codec[Select] =
    (noWait)
      .as[Select]
      .withContext("select method")

  private val selectOkCodec: Codec[SelectOk.type] =
    provide(SelectOk)
      .withContext("selectOk method")

  val all: Codec[ConfirmClass] =
    discriminated[ConfirmClass]
      .by(methodId)
      .typecase(MethodId(10), selectCodec)
      .typecase(MethodId(11), selectOkCodec)
      .withContext("confirm methods")

}