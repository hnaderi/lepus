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

package lepus.protocol.constants

import scodec.bits.ByteVector

val FrameMethod: Short = 1
val FrameHeader: Short = 2
val FrameBody: Short = 3
val FrameHeartbeat: Short = 8
val FrameMinSize: Short = 4096
val FrameEnd: Short = 206
val ProtocolHeader: ByteVector = ByteVector('A', 'M', 'Q', 'P', 0, 0, 9, 1)

enum ReplyCode(val code: Short) {
  case ReplySuccess extends ReplyCode(200)
  case ContentTooLarge extends ReplyCode(311), ErrorCode(ErrorType.Channel)
  case NoConsumers extends ReplyCode(313), ErrorCode(ErrorType.Channel)
  case ConnectionForced extends ReplyCode(320), ErrorCode(ErrorType.Connection)
  case InvalidPath extends ReplyCode(402), ErrorCode(ErrorType.Connection)
  case AccessRefused extends ReplyCode(403), ErrorCode(ErrorType.Channel)
  case NotFound extends ReplyCode(404), ErrorCode(ErrorType.Channel)
  case ResourceLocked extends ReplyCode(405), ErrorCode(ErrorType.Channel)
  case PreconditionFailed extends ReplyCode(406), ErrorCode(ErrorType.Channel)
  case FrameError extends ReplyCode(501), ErrorCode(ErrorType.Connection)
  case SyntaxError extends ReplyCode(502), ErrorCode(ErrorType.Connection)
  case CommandInvalid extends ReplyCode(503), ErrorCode(ErrorType.Connection)
  case ChannelError extends ReplyCode(504), ErrorCode(ErrorType.Connection)
  case UnexpectedFrame extends ReplyCode(505), ErrorCode(ErrorType.Connection)
  case ResourceError extends ReplyCode(506), ErrorCode(ErrorType.Connection)
  case NotAllowed extends ReplyCode(530), ErrorCode(ErrorType.Connection)
  case NotImplemented extends ReplyCode(540), ErrorCode(ErrorType.Connection)
  case InternalError extends ReplyCode(541), ErrorCode(ErrorType.Connection)
}

enum ErrorType {
  case Channel, Connection
}

sealed trait ErrorCode(val errorType: ErrorType)
