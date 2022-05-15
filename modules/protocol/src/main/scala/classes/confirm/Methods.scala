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

enum ConfirmClass(methodId: MethodId, synchronous: Boolean)
    extends Class(ClassId(85))
    with Method(methodId, synchronous) {

  case Select(nowait: NoWait)
      extends ConfirmClass(MethodId(10), true)
      with Request

  case SelectOk extends ConfirmClass(MethodId(11), true) with Response

}