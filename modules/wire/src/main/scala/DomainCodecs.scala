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

package lepus.client.codecs

import scodec.{Codec, Encoder, Decoder}
import scodec.bits.*
import scodec.codecs
import scodec.codecs.*
import lepus.protocol.*
import lepus.protocol.frame.*
import lepus.protocol.domains.*
import scodec.Attempt
import scodec.Err
import lepus.protocol.classes.basic
import lepus.protocol.constants.ReplyCode
import scodec.DecodeResult
import scodec.SizeBound

extension [T](self: Either[String, T]) {
  def asAttempt: Attempt[T] = self match {
    case Left(str) => Attempt.failure(Err(str))
    case Right(v)  => Attempt.successful(v)
  }
}
def success[T]: T => Attempt[T] = Attempt.successful

object DomainCodecs {

  lazy val channelNumber: Codec[ChannelNumber] =
    short16.xmap(ChannelNumber(_), identity)
  lazy val classId: Codec[ClassId] = short16.xmap(ClassId(_), identity)
  lazy val methodId: Codec[MethodId] = short16.xmap(MethodId(_), identity)
  lazy val consumerTag: Codec[ConsumerTag] =
    shortString.xmap(ConsumerTag(_), identity)
  lazy val deliveryTag: Codec[DeliveryTag] =
    int64.xmap(DeliveryTag(_), identity)
  lazy val shortString: Codec[ShortString] =
    variableSizeBytes(uint8, ascii).exmap(ShortString(_).asAttempt, success)
  lazy val emptyShortString: Codec[Unit] = shortString.unit(ShortString.empty)

  lazy val longString: Codec[LongString] =
    variableSizeBytesLong(uint32, ascii).exmap(LongString(_).asAttempt, success)
  lazy val emptyLongString: Codec[Unit] = longString.unit(LongString.empty)

  lazy val timestamp: Codec[Timestamp] = long(64).xmap(Timestamp(_), identity)

  lazy val decimal: Codec[Decimal] = (byte :: int32).as

  lazy val fieldData: Codec[FieldData] = lazily {
    discriminated
      .by(fixedSizeBytes(1, ascii))
      .typecase("t", bool(8))
      .typecase("b", byte)
      .typecase("B", byte)
      .typecase("s", short16)
      .typecase("u", short16)
      .typecase("I", int32)
      .typecase("i", int32)
      .typecase("l", int64)
      .typecase("L", int64) //Only when decoding
      .typecase("f", float)
      .typecase("d", double)
      .typecase("D", decimal)
      // .typecase("s", shortString)
      .typecase("S", longString)
      .typecase("T", timestamp)
      .typecase("F", fieldTable)
      .withContext("Field Table")
      .as
  }

  private lazy val fieldValuePair = shortString :: fieldData

  lazy val fieldTable: Codec[FieldTable] = codecs
    .variableSizeBytes(int32, list(fieldValuePair))
    .xmap(_.toMap, _.toList)
    .xmap(FieldTable(_), _.values)

  lazy val priority: Codec[Priority] =
    int8.exmap(Priority(_).asAttempt, success)

  lazy val deliveryMode: Codec[DeliveryMode] =
    int8
      .xmap(
        i =>
          if i != 2 then DeliveryMode.NonPersistent
          else DeliveryMode.Persistent,
        _.value
      )
      .withContext("Delivery mode")

  val flags: Codec[List[Boolean]] =
    fixedSizeBits(15, list(bool)) <~ constant(BitVector.zero)
  // 15 flag bits followed by an always false continuation flag

  lazy val basicProps: Codec[basic.Properties] =
    flags
      .flatZip { flags =>
        (
          conditional(flags(0), shortString) ::
            conditional(flags(1), shortString) ::
            conditional(flags(2), fieldTable) ::
            conditional(flags(3), deliveryMode) ::
            conditional(flags(4), priority) ::
            conditional(flags(5), shortString) ::
            conditional(flags(6), shortString) ::
            conditional(flags(7), shortString) ::
            conditional(flags(8), shortString) ::
            conditional(flags(9), timestamp) ::
            conditional(flags(10), shortString) ::
            conditional(flags(11), shortString) ::
            conditional(flags(12), shortString) ::
            conditional(flags(13), shortString)
        ).as[basic.Properties]
      }
      .xmap(_._2, p => (flagsFor(p), p))
      .withContext("Basic properties")

  /* It smells magical, but spec is too general here and the only defined properties are
   * for basic class and we implement only the required parts.*/
  private def flagsFor(p: basic.Properties): List[Boolean] =
    p.productIterator
      .map(_.asInstanceOf[Option[?]].isDefined)
      .toList
      .appended(false) // So far we have 14 flags total, so add an empty flag

  lazy val exchangeName: Codec[ExchangeName] =
    shortString.exmap(ExchangeName(_).asAttempt, success)
  lazy val queueName: Codec[QueueName] =
    shortString.exmap(QueueName(_).asAttempt, success)
  lazy val path: Codec[Path] = shortString.exmap(Path(_).asAttempt, success)

  lazy val noAck: Codec[NoAck] = bool
  lazy val noLocal: Codec[NoLocal] = bool
  lazy val noWait: Codec[NoWait] = bool
  lazy val redelivered: Codec[Redelivered] = bool
  lazy val peerProperties: Codec[PeerProperties] = fieldTable
  lazy val messageCount: Codec[MessageCount] =
    uint32.exmap(MessageCount(_).asAttempt, success)
  lazy val replyText: Codec[ReplyText] = shortString
  lazy val replyCode: Codec[ReplyCode] =
    short16.exmap(
      i =>
        ReplyCode.values
          .find(_.code == i)
          .toRight(s"Unknown reply code $i")
          .asAttempt,
      r => Attempt.successful(r.code)
    )

  inline def reverseByteAligned[T](codec: Codec[T]): Codec[T] =
    ReverseByteAlignedCodec(codec)

  /** Codec that aligned contigous bit fields into a byte, assuming that no more
    * than 8 bits are used as is the case in AMQP
    */
  private[client] final class ReverseByteAlignedCodec[T](
      codec: Codec[T]
  ) extends Codec[T] {

    def encode(t: T) =
      codec
        .encode(t)
        .map(_.padRight(8).reverseBitOrder)

    def decode(b: BitVector) =
      codec.decode(b.take(8).reverseBitOrder).map { a =>
        DecodeResult(a.value, b.drop(8))
      }
    def sizeBound = SizeBound.exact(8)
    override def toString = s"reverseByteAligned($codec)"
  }
}
