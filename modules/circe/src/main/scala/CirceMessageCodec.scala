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

package lepus.circe

import io.circe.*
import io.circe.parser.parse
import io.circe.syntax.*
import lepus.client.*
import lepus.protocol.domains.ShortString
import scodec.bits.ByteVector

import java.nio.charset.StandardCharsets

object CirceMessageCodec extends MessageCodec[Json] {
  override def decode(env: MessageRaw): Either[Throwable, Message[Json]] = for {
    txt <- env.payload.decodeUtf8
    json <- parse(txt)
  } yield env.copy(payload = json)

  override def encode(msg: Message[Json]): MessageRaw =
    val payload =
      ByteVector.view(msg.payload.noSpaces.getBytes(StandardCharsets.UTF_8))
    msg
      .copy(payload = payload)
      .withContentType(ShortString("application/json"))
}

def jsonMessageEncoder[T: Encoder]: MessageEncoder[T] =
  CirceMessageCodec.contramap(_.asJson)

def jsonMessageDecoder[T: Decoder]: MessageDecoder[T] =
  CirceMessageCodec.emap(_.as[T])

def jsonMessageCodec[T: Codec]: MessageCodec[T] =
  CirceMessageCodec.eimap(_.asJson, _.as[T])

given MessageCodec[Json] = CirceMessageCodec
