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

import cats.effect.Concurrent
import cats.effect.kernel.Deferred
import cats.syntax.all.*
import fs2.Pipe
import lepus.protocol.Frame
import lepus.protocol.domains.*

trait StartupNegotiation[F[_]] {
  def pipe: Pipe[F, Frame, Frame]
  def config: F[NegotiatedConfig]
}
object StartupNegotiation {
  def apply[F[_]: Concurrent](
      auth: AuthenticationConfig[F]
  ): F[StartupNegotiation[F]] = for {
    conf <- Deferred[F, NegotiatedConfig]
  } yield new StartupNegotiation[F] {

    override def pipe: Pipe[F, Frame, Frame] = ???

    override def config: F[NegotiatedConfig] = conf.get

  }
}

final case class NegotiatedConfig(
    channelMax: Short,
    frameMax: Int,
    heartbeat: Short
)

private final case class NegotiationStep(response: Frame, next: NegotiationStep)

private trait Negotiation[F[_]] {
  def handle(frame: Frame): F[Option[NegotiationStep]]
}
