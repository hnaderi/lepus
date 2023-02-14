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

trait EnvelopeEncoder[B] { self =>
  def encode(b: B): ByteVector
  val contentType: Option[ShortString] = None
  val contentEncoding: Option[ShortString] = None

  final def encode(msg: Message0[B]): Message0[ByteVector] = msg.copy(
    payload = encode(msg.payload),
    properties = msg.properties.copy(
      contentType = contentType,
      contentEncoding = contentEncoding
    )
  )

  final def withContentType(value: ShortString): EnvelopeEncoder[B] = new {
    def encode(b: B): ByteVector = self.encode(b)
    override val contentType: Option[ShortString] = Some(value)
    override val contentEncoding: Option[ShortString] = self.contentEncoding
  }
  final def withContentEncoding(value: ShortString): EnvelopeEncoder[B] = new {
    def encode(b: B): ByteVector = self.encode(b)
    override val contentType: Option[ShortString] = self.contentType
    override val contentEncoding: Option[ShortString] = Some(value)
  }

  final def contramap[A](f: A => B): EnvelopeEncoder[A] = new {
    override def encode(a: A): ByteVector = self.encode(f(a))
    override val contentEncoding: Option[ShortString] = self.contentEncoding
    override val contentType: Option[ShortString] = self.contentType
  }
}

object EnvelopeEncoder {
  inline def apply[T](using ee: EnvelopeEncoder[T]): EnvelopeEncoder[T] = ee
}

trait EnvelopeDecoder[A] { self =>
  def decode(
      payload: ByteVector,
      contentType: Option[ShortString] = None,
      contentEncoding: Option[ShortString] = None
  ): Either[Throwable, A]

  final def decode(msg: Message): Either[Throwable, Message0[A]] = decode(
    msg.payload,
    contentType = msg.properties.contentType,
    contentEncoding = msg.properties.contentEncoding
  ).map(a => msg.copy(payload = a))

  final def map[B](f: A => B): EnvelopeDecoder[B] = new {
    override def decode(
        payload: ByteVector,
        contentType: Option[ShortString],
        contentEncoding: Option[ShortString]
    ): Either[Throwable, B] =
      self
        .decode(
          payload,
          contentEncoding = contentEncoding,
          contentType = contentType
        )
        .map(f)
  }

  final def emap[B](f: A => Either[Throwable, B]): EnvelopeDecoder[B] = new {
    override def decode(
        payload: ByteVector,
        contentType: Option[ShortString],
        contentEncoding: Option[ShortString]
    ): Either[Throwable, B] =
      self
        .decode(
          payload,
          contentType = contentType,
          contentEncoding = contentEncoding
        )
        .flatMap(f)
  }

  final def flatMap[B](f: A => EnvelopeDecoder[B]): EnvelopeDecoder[B] = new {
    override def decode(
        payload: ByteVector,
        contentType: Option[ShortString],
        contentEncoding: Option[ShortString]
    ): Either[Throwable, B] =
      self
        .decode(
          payload,
          contentType = contentType,
          contentEncoding = contentEncoding
        )
        .flatMap(
          f(_).decode(
            payload,
            contentType = contentType,
            contentEncoding = contentEncoding
          )
        )

  }
}

object EnvelopeDecoder {
  inline def apply[T](using ed: EnvelopeDecoder[T]): EnvelopeDecoder[T] = ed
}

trait EnvelopeCodec[T] extends EnvelopeEncoder[T], EnvelopeDecoder[T]

object EnvelopeCodec {
  inline def apply[T](using ec: EnvelopeCodec[T]): EnvelopeCodec[T] = ec
}
