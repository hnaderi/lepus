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
import cats.data.NonEmptyList
import lepus.protocol.domains.LongString
import lepus.protocol.domains.ShortString

final case class SaslMechanism[F[_]](
    first: F[LongString],
    next: LongString => F[LongString]
)

/** SASL Mechanisms orderd by preferrence from high to low */
final case class AuthenticationConfig[F[_]](
    mechanisms: NonEmptyList[(ShortString, SaslMechanism[F])]
) extends AnyVal {

  /** First supported mechanism based on preferrence */
  def get(supported: String*): Option[(ShortString, SaslMechanism[F])] =
    mechanisms.foldLeft(Option.empty[(ShortString, SaslMechanism[F])]) {
      case (last @ Some(_), _) => last
      case (None, (name, mechanism)) if supported.contains(name) =>
        Some((name, mechanism))
      case _ => None
    }

}

object AuthenticationConfig {
  def apply[F[_]](
      m: (ShortString, SaslMechanism[F]),
      ms: (ShortString, SaslMechanism[F])*
  ): AuthenticationConfig[F] = new AuthenticationConfig(
    NonEmptyList.of(m, ms: _*)
  )
}
