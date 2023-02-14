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

import cats.Functor
import cats.effect.Temporal
import cats.effect.std.Console
import cats.effect.kernel.Resource
import com.comcast.ip4s.*
import fs2.io.net.Network
import lepus.protocol.domains.Path
import lepus.protocol.domains.ShortString

object LepusClient {
  def apply[F[_]: Temporal: Network: Console](
      host: Host = host"localhost",
      port: Port = port"5672",
      username: String = "guest",
      password: String = "guest",
      vhost: Path = Path("/"),
      config: ConnectionConfig = ConnectionConfig.default,
      debug: Boolean = false
  ): Resource[F, Connection[F]] =
    from(
      AuthenticationConfig.default(username = username, password = password),
      host = host,
      port = port,
      vhost = vhost,
      config = config,
      debug = debug
    )

  def from[F[_]: Temporal: Network: Console](
      auth: AuthenticationConfig[F],
      host: Host = host"localhost",
      port: Port = port"5672",
      vhost: Path = Path("/"),
      config: ConnectionConfig = ConnectionConfig.default,
      debug: Boolean = false
  ): Resource[F, Connection[F]] = {
    val transport = Transport.connect[F](SocketAddress(host, port))
    val t =
      if debug
      then Transport.debug(transport)
      else transport

    Connection.from(t, auth, path = vhost, config = config)
  }
}
