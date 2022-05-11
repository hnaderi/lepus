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

package lepus.protocol.classes

import lepus.protocol.*
import lepus.protocol.domains.*
import lepus.protocol.constants.*

enum ConnectionClass(methodId: MethodId)
    extends Class(ClassId(10))
    with Method(methodId) {

  case Start(
      versionMajor: Byte,
      versionMinor: Byte,
      serverProperties: PeerProperties,
      mechanisms: LongString,
      locales: LongString
  ) extends ConnectionClass(MethodId(10)) with Request

  case StartOk(
      clientProperties: PeerProperties,
      mechanism: ShortString,
      response: LongString,
      locale: ShortString
  ) extends ConnectionClass(MethodId(11)) with Response

  case Secure(challenge: LongString)
      extends ConnectionClass(MethodId(20))
      with Request

  case SecureOk(response: LongString)
      extends ConnectionClass(MethodId(21))
      with Response

  case Tune(channelMax: Short, frameMax: Int, heartbeat: Short)
      extends ConnectionClass(MethodId(30))
      with Request

  case TuneOk(channelMax: Short, frameMax: Int, heartbeat: Short)
      extends ConnectionClass(MethodId(31))
      with Response

  case Open(virtualHost: Path)
      extends ConnectionClass(MethodId(40))
      with Response

  case OpenOk extends ConnectionClass(MethodId(41)) with Request

  case Close(
      replyCode: ReplyCode,
      replyText: ReplyText,
      classId: ClassId,
      methodId: MethodId
  ) extends ConnectionClass(MethodId(50)) with Response with Request

  case CloseOk extends ConnectionClass(MethodId(51)) with Response with Request

  case Blocked(reason: ShortString)
      extends ConnectionClass(MethodId(60))
      with Response
      with Request

  case Unblocked
      extends ConnectionClass(MethodId(61))
      with Response
      with Request

  case UpdateSecret(newSecret: LongString, reason: ShortString)
      extends ConnectionClass(MethodId(70))
      with Request

  case UpdateSecretOk extends ConnectionClass(MethodId(71)) with Response

}
