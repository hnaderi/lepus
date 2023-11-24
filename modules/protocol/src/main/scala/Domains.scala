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

package lepus.protocol.domains

import java.time.Instant
import java.util.UUID

trait TaggedOpaqueComp[U, T <: U](using ev: U =:= T) {
  def apply(u: U): T = ev(u)
  def lift[F[_]](f: F[U]): F[T] = ev.liftCo(f)
}

opaque type ChannelNumber <: Short = Short
object ChannelNumber extends TaggedOpaqueComp[Short, ChannelNumber]

opaque type ShortString <: String = String
object ShortString extends Literally[String, ShortString] {

  inline def apply(t: String): ShortString = ${ build('t) }

  def from(uuid: UUID): ShortString = uuid.toString()
  def from(long: Long): ShortString = long.toString()

  private[lepus] inline def unsafe(str: String): ShortString = str

  def empty: ShortString = ""
  def from(str: String): Either[String, ShortString] =
    Either.cond(
      str.length <= 255,
      str,
      s"Maximum size for short strings is 255 characters! cannot create short string with length of ${str.length}"
    )
}

opaque type LongString <: String = String
private inline val LongStringSize = 4294967296L // 2 ^ 32
object LongString extends Literally[String, LongString] {

  inline def apply(t: String): LongString = ${ build('t) }

  def empty: LongString = ""
  def from(str: String): Either[String, LongString] =
    Either.cond(
      str.length <= LongStringSize,
      str,
      s"Maximum size for long strings is $LongStringSize characters! cannot create short string with length of ${str.length}"
    )
}

opaque type Timestamp <: Long = Long
object Timestamp {
  def apply(t: Long): Timestamp = t
  extension (t: Timestamp) {
    def toInstant: Instant = Instant.ofEpochMilli(t)
  }
  def from(instant: Instant): Timestamp = instant.toEpochMilli()
}

final case class Decimal(scale: Byte, value: Int)

type FieldData =
  ShortString | LongString | Boolean | Byte | Short | Int | Long | Float |
    Double | Decimal | Timestamp | FieldTable

final case class FieldTable(values: Map[ShortString, FieldData])
    extends AnyVal {
  def get(key: ShortString): Option[FieldData] = values.get(key)
  def updated(key: ShortString, value: FieldData): FieldTable = FieldTable(
    values.updated(key, value)
  )
  def updated(key: ShortString, value: Option[FieldData]): FieldTable =
    value.fold(this)(this.updated(key, _))
}
object FieldTable {
  def apply(entries: (ShortString, FieldData)*): FieldTable = new FieldTable(
    entries.toMap
  )
  val empty: FieldTable = FieldTable(Map.empty)
}

opaque type ClassId <: Short = Short
object ClassId extends TaggedOpaqueComp[Short, ClassId]

/** Identifier for the consumer, valid within the current c hannel.
  */
opaque type ConsumerTag <: ShortString = ShortString
object ConsumerTag extends TaggedOpaqueComp[ShortString, ConsumerTag] {
  inline def of(inline t: String): ConsumerTag = ShortString(t)
  def empty: ConsumerTag = ShortString.empty
  def from(t: String): Either[String, ConsumerTag] = ShortString.from(t)
  def random: ConsumerTag = ShortString.from(UUID.randomUUID)
}

/** The server-assigned and channel-specific delivery tag
  */
opaque type DeliveryTag <: Long = Long
object DeliveryTag extends TaggedOpaqueComp[Long, DeliveryTag]

private val namePattern = "^[a-zA-Z0-9-_.:]*$".r
private def validateName(name: String): Either[String, String] =
  Either.cond(namePattern.matches(name), name, "Invalid name!")
private def validateNameSize(name: String): Either[String, String] =
  Either.cond(
    name.length <= 127,
    name,
    "Maximum name length is 127 characters!"
  )

/** The exchange name is a client-selected string that identifies the exchange
  * for publish methods.
  */
opaque type ExchangeName <: ShortString = String
object ExchangeName extends Literally[String, ExchangeName] {
  val default: ExchangeName = ""

  inline def apply(t: String): ExchangeName = ${ build('t) }

  def from(name: String): Either[String, ExchangeName] =
    validateName(name).flatMap(validateNameSize)
}

opaque type MethodId <: Short = Short
object MethodId extends TaggedOpaqueComp[Short, MethodId]

/** If this field is set the server does not expect acknowledgements for
  * messages. That is, when a message is delivered to the client the server
  * assumes the delivery will succeed and immediately dequeues it. This
  * functionality may increase performance but at the cost of reliability.
  * Messages can get lost if a client dies before they are delivered to the
  * application.
  */
type NoAck = Boolean

/** If the no-local field is set the server will not send messages to the
  * connection that published them.
  */
type NoLocal = Boolean

/** If set, the server will not respond to the method. The client should not
  * wait for a reply method. If the server could not complete the method it will
  * raise a channel or connection exception.
  */
type NoWait = Boolean

/** Unconstrained.
  */
opaque type Path <: ShortString = String
object Path extends Literally[String, Path] {

  inline def apply(t: String): Path = ${ build('t) }

  def from(str: String): Either[String, Path] =
    Either.cond(str.length <= 127, str, "Maximum length is 127 characters!")
}

/** This table provides a set of peer properties, used for identification,
  * debugging, and general information.
  */
type PeerProperties = FieldTable

/** The queue name identifies the queue within the vhost. In methods where the
  * queue name may be blank, and that has no specific significance, this refers
  * to the 'current' queue for the channel, meaning the last queue that the
  * client declared on the channel. If the client did not declare a queue, and
  * the method needs a queue name, this will result in a 502 (syntax error)
  * channel exception.
  */
opaque type QueueName <: ShortString = String
object QueueName extends Literally[String, QueueName] {
  val autoGen: QueueName = ""

  inline def apply(t: String): QueueName = ${ build('t) }

  def from(name: String): Either[String, QueueName] =
    validateName(name).flatMap(validateNameSize)
}

/** This indicates that the message has been previously delivered to this or
  * another client.
  */
type Redelivered = Boolean

/** The number of messages in the queue, which will be zero for newly-declared
  * queues. This is the number of messages present in the queue, and committed
  * if the channel on which they were published is transacted, that are not
  * waiting acknowledgement.
  */
opaque type MessageCount <: Long = Long
object MessageCount extends Literally[Long, MessageCount] {

  inline def apply(t: Long): MessageCount = ${ build('t) }

  def from(count: Long): Either[String, MessageCount] =
    Either.cond(count >= 0, count, "Count cannot be negative!")
}

/** The localised reply text. This text can be logged as an aid to resolving
  * issues.
  */
type ReplyText = ShortString

enum DeliveryMode(val value: Byte) {
  case NonPersistent extends DeliveryMode(1)
  case Persistent extends DeliveryMode(2)
}

type Priority = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9
object Priority extends Literally[Int, Priority] {

  inline def apply(t: Int): Priority = ${ build('t) }

  def from(b: Int): Either[String, Priority] = b match {
    case p: Priority => Right(p)
    case _           => Left("Valid priorities are 0-9")
  }
}

opaque type ExchangeType <: ShortString = ShortString
object ExchangeType extends Literally[String, ExchangeType] {
  inline def apply(t: String): ExchangeType = ${ build('t) }

  override def from(str: String): Either[String, ExchangeType] =
    ShortString.from(str)

  val Direct: ExchangeType = "direct"
  val Topic: ExchangeType = "topic"
  val Fanout: ExchangeType = "fanout"
  val Headers: ExchangeType = "headers"
}
