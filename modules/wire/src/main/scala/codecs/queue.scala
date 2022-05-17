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

package lepus.wire

import lepus.protocol.*
import lepus.protocol.domains.*
import lepus.protocol.*
import lepus.protocol.QueueClass.*
import lepus.protocol.constants.*
import lepus.wire.DomainCodecs.*
import scodec.{Codec, Encoder, Decoder}
import scodec.codecs.*

object QueueCodecs {

  private val declareCodec: Codec[Declare] =
    ((short16.unit(0) :: queueName) ++ (reverseByteAligned(
      bool :: bool :: bool :: bool :: noWait
    ) :+ (fieldTable)))
      .as[Declare]
      .withContext("declare method")

  private val declareOkCodec: Codec[DeclareOk] =
    (queueName :: messageCount :: int32)
      .as[DeclareOk]
      .withContext("declareOk method")

  private val bindCodec: Codec[Bind] =
    ((short16.unit(
      0
    ) :: queueName :: exchangeName :: shortString) ++ (reverseByteAligned(
      noWait
    ) :: (fieldTable)))
      .as[Bind]
      .withContext("bind method")

  private val bindOkCodec: Codec[BindOk.type] =
    provide(BindOk)
      .withContext("bindOk method")

  private val unbindCodec: Codec[Unbind] =
    (short16.unit(0) :: queueName :: exchangeName :: shortString :: fieldTable)
      .as[Unbind]
      .withContext("unbind method")

  private val unbindOkCodec: Codec[UnbindOk.type] =
    provide(UnbindOk)
      .withContext("unbindOk method")

  private val purgeCodec: Codec[Purge] =
    (short16.unit(0) :: queueName :: (reverseByteAligned(noWait)))
      .as[Purge]
      .withContext("purge method")

  private val purgeOkCodec: Codec[PurgeOk] =
    (messageCount)
      .as[PurgeOk]
      .withContext("purgeOk method")

  private val deleteCodec: Codec[Delete] =
    ((short16.unit(0) :: queueName) ++ (reverseByteAligned(
      bool :: bool :: noWait
    )))
      .as[Delete]
      .withContext("delete method")

  private val deleteOkCodec: Codec[DeleteOk] =
    (messageCount)
      .as[DeleteOk]
      .withContext("deleteOk method")

  val all: Codec[QueueClass] =
    discriminated[QueueClass]
      .by(methodId)
      .subcaseP[Declare](MethodId(10)) { case m: Declare => m }(declareCodec)
      .subcaseP[DeclareOk](MethodId(11)) { case m: DeclareOk => m }(
        declareOkCodec
      )
      .subcaseP[Bind](MethodId(20)) { case m: Bind => m }(bindCodec)
      .subcaseP[BindOk.type](MethodId(21)) { case m: BindOk.type => m }(
        bindOkCodec
      )
      .subcaseP[Unbind](MethodId(50)) { case m: Unbind => m }(unbindCodec)
      .subcaseP[UnbindOk.type](MethodId(51)) { case m: UnbindOk.type => m }(
        unbindOkCodec
      )
      .subcaseP[Purge](MethodId(30)) { case m: Purge => m }(purgeCodec)
      .subcaseP[PurgeOk](MethodId(31)) { case m: PurgeOk => m }(purgeOkCodec)
      .subcaseP[Delete](MethodId(40)) { case m: Delete => m }(deleteCodec)
      .subcaseP[DeleteOk](MethodId(41)) { case m: DeleteOk => m }(deleteOkCodec)
      .withContext("queue methods")

}
