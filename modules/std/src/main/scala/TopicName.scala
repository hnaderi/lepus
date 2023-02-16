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
import lepus.protocol.domains.ShortString

import scala.annotation.targetName
import scala.compiletime.*
import scala.deriving.Mirror
import scala.quoted.*

opaque type TopicName <: ShortString = ShortString
object TopicName {
  private val validPattern = "^(?:[A-Za-z0-9]+\\.)*[A-Za-z0-9]*$".r

  private final def build(t: Expr[String])(using ctx: Quotes): Expr[TopicName] =
    import ctx.reflect.report
    t.value match {
      case Some(v) => from(v).fold(report.errorAndAbort, Expr(_))
      case None    => report.errorAndAbort("Not a literal value!")
    }

  inline def apply(inline name: String): TopicName = ${ build('name) }

  def from(name: String): Either[String, TopicName] =
    ShortString.from(name).flatMap {
      case str if validPattern.matches(str) => ShortString.from(str)
      case bad => Left(s"Invalid topic name `$bad`!")
    }

  extension (topic: TopicName) {
    inline def selector: TopicSelector = TopicSelector.exact(topic)
  }
}
final class TopicNameEncoder[A](private val build: A => String) extends AnyVal {
  def get: A => Either[String, TopicName] = build.andThen(TopicName.from)
  def contramap[B](f: B => A): TopicNameEncoder[B] =
    TopicNameEncoder(build.compose(f))

  def prefixed(prefix: String): TopicNameEncoder[A] =
    TopicNameEncoder(a => prefix + build(a))

  def transform(f: String => String): TopicNameEncoder[A] =
    TopicNameEncoder(build.andThen(f))
}

object TopicNameEncoder {
  def from[A](f: A => String): TopicNameEncoder[A] = TopicNameEncoder(f)

  inline def of[T](using m: Mirror.Of[T]): TopicNameEncoder[T] =
    inline m match
      case s: Mirror.SumOf[T]     => sumInst(s)
      case p: Mirror.ProductOf[T] => productInst(p)

  private inline def productInst[T](
      m: Mirror.ProductOf[T]
  ): TopicNameEncoder[T] =
    TopicNameEncoder(_ => TopicName(constValue[m.MirroredLabel]))

  private inline def summonAll[T <: Tuple]: List[TopicNameEncoder[?]] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (h *: t)   => of(using summonInline[Mirror.Of[h]]) +: summonAll[t]
    }

  private inline def sumInst[T](
      m: Mirror.SumOf[T]
  ): TopicNameEncoder[T] = {
    val enc = summonAll[m.MirroredElemTypes]
    TopicNameEncoder(t =>
      enc(m.ordinal(t)).asInstanceOf[TopicNameEncoder[T]].build(t)
    )
  }

}
