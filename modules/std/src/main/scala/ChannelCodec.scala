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

package lepus.std

import cats.syntax.all.*
import dev.hnaderi.namedcodec.*
import lepus.client.*
import lepus.protocol.domains.*

object ChannelCodec {
  def of[T, R](using
      nc: NamedCodec[T, R],
      enc: MessageEncoder[R],
      dec: MessageDecoder[R]
  ): MessageCodec[T] = new {

    override def encode(msg: Message[T]): Either[Throwable, MessageRaw] = {
      val typed = nc.encode(msg.payload)
      ShortString
        .from(typed.name)
        .leftMap(BadMessageType(typed.name, _))
        .flatMap(msgType =>
          enc.encode(msg.withPayload(typed.data).withMsgType(msgType))
        )
    }

    override def decode(msg: MessageRaw): Either[Throwable, Message[T]] = for {
      ir <- dec.decode(msg)
      msgType <- msg.properties.msgType.toRight(NoMessageTypeFound)
      decoded <- nc
        .decode(EncodedMessage(msgType, ir.payload))
        .leftMap(DecodeFailure(_))
    } yield msg.withPayload(decoded)

  }

  final case class BadMessageType(value: String, details: String)
      extends RuntimeException(
        s"Cannot create message type from $value, because: $details"
      )
  case object NoMessageTypeFound
      extends RuntimeException("Message type is required!")

  final case class DecodeFailure(msg: String) extends RuntimeException(msg)

}
