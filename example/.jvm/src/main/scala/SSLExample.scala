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

import cats.effect.IO
import cats.effect.IOApp
import com.comcast.ip4s.*
import lepus.client.*

import javax.net.ssl.SSLContext

object SSLExample extends IOApp.Simple {

  val connect =
    LepusClient[IO](
      debug = true,
      port = port"5671",
      ssl = SSL.fromSSLContext(sslContext)
    )

  override def run: IO[Unit] = connect.use(Main.app)

  // example from https://www.rabbitmq.com/ssl.html#java-client-connecting-with-peer-verification
  private def sslContext: SSLContext = {
    import java.io.*
    import java.security.*
    import javax.net.ssl.*

    val keyPassphrase = "lepus".toArray
    val ks = KeyStore.getInstance("PKCS12")
    ks.load(new FileInputStream("../../develop/keys/client.p12"), keyPassphrase)

    val kmf = KeyManagerFactory.getInstance("SunX509")
    kmf.init(ks, keyPassphrase)

    val trustPassphrase = "keystore password".toCharArray()
    val tks = KeyStore.getInstance("JKS")
    tks.load(
      new FileInputStream("../../develop/keys/keystore.jks"),
      trustPassphrase
    )

    val tmf = TrustManagerFactory.getInstance("SunX509")
    tmf.init(tks)

    val c = SSLContext.getInstance("TLSv1.2")
    c.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null)

    return c
  }
}
