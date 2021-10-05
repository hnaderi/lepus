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
import cats.instances.lazyList

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
    long(32).xmap(DeliveryTag(_), identity)
  lazy val shortString: Codec[ShortString] =
    variableSizeBytes(int8, ascii).exmap(ShortString(_).asAttempt, success)
  lazy val emptyShortString: Codec[Unit] = shortString.unit(ShortString.empty)

  lazy val longString: Codec[LongString] =
    variableSizeBytes(int32, ascii).exmap(LongString(_).asAttempt, success)
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
      .typecase("L", long(64)) //FIXME when encoding?
      .typecase("l", long(64))
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
  lazy val noWait: Codec[NoWait] = bool(4) //HACK?
  lazy val peerProperties: Codec[PeerProperties] = fieldTable
  lazy val redelivered: Codec[Redelivered] = bool
  lazy val messageCount: Codec[MessageCount] = int16
  lazy val replyText: Codec[ReplyText] = shortString
  lazy val replyCode: Codec[ReplyCode] =
    ignore(16) ~> provide(ReplyCode.AccessRefused)
}
