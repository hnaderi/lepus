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

package lepus.client.internal

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.syntax.all.*
import lepus.protocol.Method
import munit.CatsEffectAssertions.*

import FakeRPCChannel.Interaction

final class FakeRPCChannel(
    val interactions: InteractionList[Interaction],
    val error: PlannedError
) extends RPCChannel[IO] {

  override def sendWait(m: Method): IO[Method] =
    interact(Interaction.SendWait(m)).as(m)

  override def sendNoWait(m: Method): IO[Unit] = interact(
    Interaction.SendNoWait(m)
  )

  override def recv(m: Method): IO[Unit] = interact(Interaction.Recv(m))

  private def interact(value: Interaction) =
    interactions.add(value) >> error.run
}

object FakeRPCChannel {
  enum Interaction {
    case SendWait(method: Method)
    case SendNoWait(method: Method)
    case Recv(method: Method)
  }
  def apply(): IO[FakeRPCChannel] =
    (InteractionList[Interaction], PlannedError())
      .mapN(new FakeRPCChannel(_, _))
}
