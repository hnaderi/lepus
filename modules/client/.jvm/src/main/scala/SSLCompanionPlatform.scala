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
import fs2.io.net.Network
import fs2.io.net.tls.TLSContext

import java.nio.file.Path
import java.security.KeyStore
import javax.net.ssl.SSLContext

private[client] transparent trait SSLCompanionPlatform { this: SSL.type =>

  /** Creates a `SSL` from an `SSLContext`. */
  def fromSSLContext(ctx: SSLContext): SSL =
    new SSL {
      def tlsContext[F[_]: Network](implicit
          ev: ApplicativeError[F, Throwable]
      ): Resource[F, TLSContext[F]] =
        Resource.pure(Network[F].tlsContext.fromSSLContext(ctx))
    }

  /** Creates a `SSL` from the specified key store file. */
  def fromKeyStoreFile(
      file: Path,
      storePassword: Array[Char],
      keyPassword: Array[Char]
  ): SSL =
    new SSL {
      def tlsContext[F[_]: Network](implicit
          ev: ApplicativeError[F, Throwable]
      ): Resource[F, TLSContext[F]] =
        Resource.eval(
          Network[F].tlsContext
            .fromKeyStoreFile(file, storePassword, keyPassword)
        )
    }

  /** Creates a `SSL` from the specified class path resource. */
  def fromKeyStoreResource(
      resource: String,
      storePassword: Array[Char],
      keyPassword: Array[Char]
  ): SSL =
    new SSL {
      def tlsContext[F[_]: Network](implicit
          ev: ApplicativeError[F, Throwable]
      ): Resource[F, TLSContext[F]] =
        Resource.eval(
          Network[F].tlsContext
            .fromKeyStoreResource(resource, storePassword, keyPassword)
        )
    }

  /** Creates a `TLSContext` from the specified key store. */
  def fromKeyStore(
      keyStore: KeyStore,
      keyPassword: Array[Char]
  ): SSL =
    new SSL {
      def tlsContext[F[_]: Network](implicit
          ev: ApplicativeError[F, Throwable]
      ): Resource[F, TLSContext[F]] =
        Resource.eval(Network[F].tlsContext.fromKeyStore(keyStore, keyPassword))
    }
}
