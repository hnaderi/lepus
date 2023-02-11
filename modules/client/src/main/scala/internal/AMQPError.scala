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

package lepus.client.internal

import lepus.protocol.*
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*

private[client] final case class AMQPError(
    replyCode: ReplyCode,
    replyText: ReplyText,
    classId: ClassId,
    methodId: MethodId
) extends Exception(
      s"""Protocol error detected!
code: $replyCode
msg:  $replyText
class: $classId
method: $methodId
"""
    )

private[client] final case class UnexpectedResponse(
    response: Method,
    expectedClass: ClassId,
    expectedMethod: MethodId
) extends RuntimeException(s"""Received unexpected method response from server!
Expected class: $expectedClass method: $expectedMethod
Received $response
""")
