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

enum ChannelClass(methodId: MethodId)
    extends Class(ClassId(20))
    with Method(methodId) {

  case Open extends ChannelClass(MethodId(10)) with Request

  case OpenOk extends ChannelClass(MethodId(11)) with Response

  case Flow(active: Boolean)
      extends ChannelClass(MethodId(20))
      with Request
      with Response

  case FlowOk(active: Boolean)
      extends ChannelClass(MethodId(21))
      with Request
      with Response

  case Close(
      replyCode: ReplyCode,
      replyText: ReplyText,
      classId: ClassId,
      methodId: MethodId
  ) extends ChannelClass(MethodId(40)) with Request with Response

  case CloseOk extends ChannelClass(MethodId(41)) with Request with Response

}
