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

import cats.effect.IO
import cats.effect.Resource
import com.comcast.ip4s.*
import munit.CatsEffectSuite
import org.testcontainers.containers.RabbitMQContainer

trait BrokerSuite extends CatsEffectSuite {

  val rabbit = ResourceSuiteLocalFixture(
    "rabbitmq",
    Resource.make(IO.blocking {
      val c = new RabbitMQContainer("rabbitmq:3")
      c.start()
      c
    })(c => IO.blocking(c.stop()))
  )

  override def munitFixtures = List(rabbit)

  def connection: Resource[IO, Connection[IO]] =
    LepusClient[IO](
      host = Host.fromString(rabbit().getHost).getOrElse(host"localhost"),
      port = Port.fromInt(rabbit().getAmqpPort).getOrElse(port"5672"),
      username = rabbit().getAdminUsername,
      password = rabbit().getAdminPassword
    )
}
