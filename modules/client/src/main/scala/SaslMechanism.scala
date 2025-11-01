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

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.syntax.all.*
import lepus.protocol.domains.LongString
import lepus.protocol.domains.ShortString

final case class SaslMechanism[F[_]](
    name: ShortString,
    first: F[LongString],
    next: LongString => F[LongString]
)

object SaslMechanism {
  // TODO Sasl prep and stuff...
  def plain[F[_]: MonadThrow](username: String, password: String) = {
    val s = LongString
      .from(s"\u0000$username\u0000$password")
      .leftMap(new Exception(_))
      .liftTo[F]
    SaslMechanism(
      ShortString("PLAIN"),
      s,
      _ =>
        new IllegalStateException(
          "SASL plain is a single step mechanism"
        ).raiseError
    )
  }
}

/** SASL Mechanisms orderd by preferrence from high to low */
final class AuthenticationConfig[F[_]](
    val mechanisms: NonEmptyList[SaslMechanism[F]]
) extends AnyVal {

  /** First supported mechanism based on preferrence */
  def get(supported: String*): Option[SaslMechanism[F]] =
    mechanisms.foldLeft(Option.empty[SaslMechanism[F]]) {
      case (last @ Some(_), _)                                     => last
      case (None, mechanism) if supported.contains(mechanism.name) =>
        Some(mechanism)
      case _ => None
    }

}

object AuthenticationConfig {
  def apply[F[_]](
      m: SaslMechanism[F],
      ms: SaslMechanism[F]*
  ): AuthenticationConfig[F] = new AuthenticationConfig(
    NonEmptyList.of(m, ms: _*)
  )

  def default[F[_]: MonadThrow](
      username: String,
      password: String
  ): AuthenticationConfig[F] = apply(SaslMechanism.plain(username, password))
}
