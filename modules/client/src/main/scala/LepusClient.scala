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

import cats.effect.Temporal
import cats.effect.kernel.Resource
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import com.comcast.ip4s.SocketAddress
import fs2.io.net.Network
import lepus.protocol.domains.Path
import lepus.protocol.domains.ShortString

object LepusClient {
  def apply[F[_]: Temporal: Network: cats.effect.std.Console](
      host: Host,
      port: Port,
      username: String,
      password: String,
      vhost: Path = Path("/")
  ): Resource[F, Connection[F]] = {
    val transport = Transport
      .connect[F](SocketAddress(host, port))
      .andThen(_.through(Transport.debug(true)))
      .compose(Transport.debug(false))

    Connection.from(
      transport,
      AuthenticationConfig(
        ShortString("PLAIN") -> SaslMechanism.plain(username, password)
      ),
      path = vhost
    )
  }
}
