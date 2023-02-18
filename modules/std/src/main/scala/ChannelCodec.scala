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

/** ChannelCodec is the interface for encoding/decoding operations for messages
  * that can fail on both ways.
  *
  * Note that this is not a typeclass and it is not used in implicit scope and
  * there are no instances of it.
  */
trait ChannelCodec[T] {
  def encode(msg: Message[T]): Either[Throwable, MessageRaw]
  final def encode(payload: T): Either[Throwable, MessageRaw] =
    encode(Message(payload))
  def decode(msg: MessageRaw): Either[Throwable, Message[T]]
}

object ChannelCodec {
  def default[T, R](codec: NamedCodec[T, R])(using
      enc: MessageEncoder[R],
      dec: MessageDecoder[R]
  ): ChannelCodec[T] = new {

    override def encode(msg: Message[T]): Either[Throwable, MessageRaw] = {
      val typed = codec.encode(msg.payload)
      ShortString
        .from(typed.name)
        .leftMap(BadMessageType(typed.name, _))
        .map(msgType =>
          enc.encode(msg.withPayload(typed.data).withMsgType(msgType))
        )
    }

    override def decode(msg: MessageRaw): Either[Throwable, Message[T]] = for {
      ir <- dec.decode(msg)
      msgType <- msg.properties.msgType.toRight(NoMessageTypeFound)
      decoded <- codec
        .decode(EncodedMessage(msgType, ir.payload))
        .leftMap(DecodeFailure(_))
    } yield msg.withPayload(decoded)

  }

  def plain[T](using
      enc: MessageEncoder[T],
      dec: MessageDecoder[T]
  ): ChannelCodec[T] = new {
    override def encode(msg: Message[T]): Either[Throwable, MessageRaw] =
      Right(enc.encode(msg))
    override def decode(msg: MessageRaw): Either[Throwable, Message[T]] =
      dec.decode(msg)
  }

  def plain[T](codec: MessageCodec[T]): ChannelCodec[T] = new {
    override def encode(msg: Message[T]): Either[Throwable, MessageRaw] =
      Right(codec.encode(msg))
    override def decode(msg: MessageRaw): Either[Throwable, Message[T]] =
      codec.decode(msg)
  }

  final case class BadMessageType(value: String, details: String)
      extends RuntimeException(
        s"Cannot create message type from $value, because: $details"
      )
  case object NoMessageTypeFound
      extends RuntimeException("Message type is required!")

  final case class DecodeFailure(msg: String) extends RuntimeException(msg)

}
