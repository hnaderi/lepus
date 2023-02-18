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

package lepus.client

import lepus.protocol.domains.*
import scodec.bits.ByteVector

import java.nio.charset.StandardCharsets
import scala.annotation.implicitNotFound

@implicitNotFound("Cannot find a way to encode ${B} into a message")
trait MessageEncoder[B] { self =>
  def encode(msg: Message[B]): MessageRaw
  final def encode(payload: B): MessageRaw = encode(Message(payload))
  final def encode(env: Envelope[B]): EnvelopeRaw =
    env.copy(message = encode(env.message))

  final def contramap[A](f: A => B): MessageEncoder[A] = new {
    override def encode(msg: Message[A]): MessageRaw =
      self.encode(msg.copy(payload = f(msg.payload)))
  }

  final def postEncode(f: MessageRaw => MessageRaw): MessageEncoder[B] = new {
    override def encode(msg: Message[B]): MessageRaw =
      f(self.encode(msg))
  }
}
object MessageEncoder {
  inline def apply[T](using enc: MessageEncoder[T]): MessageEncoder[T] = enc
  given MessageEncoder[String] = new {
    override def encode(msg: Message[String]): MessageRaw =
      msg
        .copy(payload =
          ByteVector.view(msg.payload.getBytes(StandardCharsets.UTF_8))
        )
        .withContentType(ShortString("text/plain"))
  }
}

@implicitNotFound("Cannot find a way to decode a message into ${A}")
trait MessageDecoder[A] { self =>
  def decode(env: MessageRaw): Either[Throwable, Message[A]]
  final def decode(env: EnvelopeRaw): Either[Throwable, Envelope[A]] =
    decode(env.message).map(m => env.copy(message = m))

  final def map[B](f: A => B): MessageDecoder[B] = new {
    override def decode(env: MessageRaw): Either[Throwable, Message[B]] =
      self.decode(env).map(m => m.copy(payload = f(m.payload)))
  }

  final def emap[B](f: A => Either[Throwable, B]): MessageDecoder[B] = new {
    override def decode(env: MessageRaw): Either[Throwable, Message[B]] =
      self
        .decode(env)
        .flatMap(msg => f(msg.payload).map(p => msg.copy(payload = p)))
  }

  final def mapMessage[B](f: Message[A] => Message[B]): MessageDecoder[B] =
    new {
      override def decode(env: MessageRaw): Either[Throwable, Message[B]] =
        self.decode(env).map(f)
    }
  final def emapMessage[B](
      f: Message[A] => Either[Throwable, Message[B]]
  ): MessageDecoder[B] = new {
    override def decode(env: MessageRaw): Either[Throwable, Message[B]] =
      self.decode(env).flatMap(f)
  }
}
object MessageDecoder {
  inline def apply[T](using dec: MessageDecoder[T]): MessageDecoder[T] = dec
  given MessageDecoder[String] = new {
    override def decode(env: MessageRaw): Either[Throwable, Message[String]] =
      env.payload.decodeUtf8.map(p => env.copy(payload = p))
  }
}

trait MessageCodec[T] extends MessageDecoder[T], MessageEncoder[T] { self =>
  def imap[A](in: A => T, out: T => A): MessageCodec[A] = new {
    private val enc = self.contramap(in)
    private val dec = self.map(out)

    export enc.encode
    export dec.decode
  }
  def eimap[A](in: A => T, out: T => Either[Throwable, A]): MessageCodec[A] =
    new {
      private val enc = self.contramap(in)
      private val dec = self.emap(out)

      export enc.encode
      export dec.decode
    }
}

object MessageCodec {
  inline def apply[T](using codec: MessageCodec[T]): MessageCodec[T] = codec
  def of[T](using
      enc: MessageEncoder[T],
      dec: MessageDecoder[T]
  ): MessageCodec[T] = new {
    export enc.encode
    export dec.decode
  }

  inline given [T: MessageEncoder: MessageDecoder]: MessageCodec[T] = of[T]
}
