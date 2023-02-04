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

enum ReplyCode(val code: Short, val category: ReplyCategory) {
  case ReplySuccess extends ReplyCode(200, ReplyCategory.Success)
  case ContentTooLarge extends ReplyCode(311, ReplyCategory.ChannelError)
  case NoConsumers extends ReplyCode(313, ReplyCategory.ChannelError)
  case ConnectionForced extends ReplyCode(320, ReplyCategory.ConnectionError)
  case InvalidPath extends ReplyCode(402, ReplyCategory.ConnectionError)
  case AccessRefused extends ReplyCode(403, ReplyCategory.ChannelError)
  case NotFound extends ReplyCode(404, ReplyCategory.ChannelError)
  case ResourceLocked extends ReplyCode(405, ReplyCategory.ChannelError)
  case PreconditionFailed extends ReplyCode(406, ReplyCategory.ChannelError)
  case FrameError extends ReplyCode(501, ReplyCategory.ConnectionError)
  case SyntaxError extends ReplyCode(502, ReplyCategory.ConnectionError)
  case CommandInvalid extends ReplyCode(503, ReplyCategory.ConnectionError)
  case ChannelError extends ReplyCode(504, ReplyCategory.ConnectionError)
  case UnexpectedFrame extends ReplyCode(505, ReplyCategory.ConnectionError)
  case ResourceError extends ReplyCode(506, ReplyCategory.ConnectionError)
  case NotAllowed extends ReplyCode(530, ReplyCategory.ConnectionError)
  case NotImplemented extends ReplyCode(540, ReplyCategory.ConnectionError)
  case InternalError extends ReplyCode(541, ReplyCategory.ConnectionError)
}

enum ReplyCategory {
  case Success, ChannelError, ConnectionError
}
