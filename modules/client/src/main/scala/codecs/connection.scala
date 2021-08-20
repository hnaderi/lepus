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
import lepus.protocol.classes.ConnectionClass.*
import lepus.protocol.constants.*
import lepus.client.codecs.DomainCodecs.*
import scodec.{Codec, Encoder, Decoder}
import scodec.codecs.*

object ConnectionCodecs {

  private val startCodec: Codec[Start] =
    (byte :: byte :: peerProperties :: longString :: longString)
      .as[Start]
      .withContext("start method")

  private val startOkCodec: Codec[StartOk] =
    (peerProperties :: shortString :: longString :: shortString)
      .as[StartOk]
      .withContext("startOk method")

  private val secureCodec: Codec[Secure] =
    (longString)
      .as[Secure]
      .withContext("secure method")

  private val secureOkCodec: Codec[SecureOk] =
    (longString)
      .as[SecureOk]
      .withContext("secureOk method")

  private val tuneCodec: Codec[Tune] =
    (short16 :: int32 :: short16)
      .as[Tune]
      .withContext("tune method")

  private val tuneOkCodec: Codec[TuneOk] =
    (short16 :: int32 :: short16)
      .as[TuneOk]
      .withContext("tuneOk method")

  private val openCodec: Codec[Open] =
    (path)
      .as[Open]
      .withContext("open method")

  private val openOkCodec: Codec[OpenOk.type] =
    provide(OpenOk)
      .withContext("openOk method")

  private val closeCodec: Codec[Close] =
    (replyCode :: replyText :: classId :: methodId)
      .as[Close]
      .withContext("close method")

  private val closeOkCodec: Codec[CloseOk.type] =
    provide(CloseOk)
      .withContext("closeOk method")

  private val blockedCodec: Codec[Blocked] =
    (shortString)
      .as[Blocked]
      .withContext("blocked method")

  private val unblockedCodec: Codec[Unblocked.type] =
    provide(Unblocked)
      .withContext("unblocked method")

  private val updateSecretCodec: Codec[UpdateSecret] =
    (longString :: shortString)
      .as[UpdateSecret]
      .withContext("updateSecret method")

  private val updateSecretOkCodec: Codec[UpdateSecretOk.type] =
    provide(UpdateSecretOk)
      .withContext("updateSecretOk method")

  val all: Codec[ConnectionClass] =
    discriminated[ConnectionClass]
      .by(methodId)
      .typecase(MethodId(10), startCodec)
      .typecase(MethodId(11), startOkCodec)
      .typecase(MethodId(20), secureCodec)
      .typecase(MethodId(21), secureOkCodec)
      .typecase(MethodId(30), tuneCodec)
      .typecase(MethodId(31), tuneOkCodec)
      .typecase(MethodId(40), openCodec)
      .typecase(MethodId(41), openOkCodec)
      .typecase(MethodId(50), closeCodec)
      .typecase(MethodId(51), closeOkCodec)
      .typecase(MethodId(60), blockedCodec)
      .typecase(MethodId(61), unblockedCodec)
      .typecase(MethodId(70), updateSecretCodec)
      .typecase(MethodId(71), updateSecretOkCodec)
      .withContext("connection methods")

}