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

import cats._
import cats.effect.Resource
import cats.syntax.all._
import fs2.io.net.Network
import fs2.io.net.tls.TLSContext
import fs2.io.net.tls.TLSParameters

abstract class SSL private[lepus] (
    val tlsParameters: TLSParameters = TLSParameters.Default
) { outer =>
  def tlsContext[F[_]: Network](implicit
      ev: ApplicativeError[F, Throwable]
  ): Resource[F, TLSContext[F]]

  final def withTLSParameters(tlsParameters: TLSParameters): SSL =
    new SSL(tlsParameters) {
      def tlsContext[F[_]: Network](implicit
          ev: ApplicativeError[F, Throwable]
      ): Resource[F, TLSContext[F]] =
        outer.tlsContext
    }
}

object SSL extends SSLCompanionPlatform {

  /** `SSL` which indicates that SSL is not to be used. */
  object None extends SSL {
    def tlsContext[F[_]: Network](implicit
        ev: ApplicativeError[F, Throwable]
    ): Resource[F, TLSContext[F]] =
      Resource.eval(
        ev.raiseError(new Exception("SSL.None: cannot create a TLSContext."))
      )
  }

  /** `SSL` which trusts all certificates. */
  object Trusted extends SSL {
    def tlsContext[F[_]: Network](implicit
        ev: ApplicativeError[F, Throwable]
    ): Resource[F, TLSContext[F]] =
      Network[F].tlsContext.insecureResource
  }

  /** `SSL` from the system default `SSLContext`. */
  object System extends SSL {
    def tlsContext[F[_]: Network](implicit
        ev: ApplicativeError[F, Throwable]
    ): Resource[F, TLSContext[F]] =
      Network[F].tlsContext.systemResource
  }

}
