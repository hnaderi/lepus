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

import lepus.protocol.domains.ShortString
import scala.annotation.targetName

final class ChannelConfig private (
    val returnedBufSize: Int = 10,
    val confirmBufSize: Int = 10,
    val maxConcurrentGet: Int = 10,
    val maxConcurrentRPC: Int = 10,
    val maxConcurrentPublish: Int = 10
) {
  private def copy(
      returnedBufSize: Int = returnedBufSize,
      confirmBufSize: Int = confirmBufSize,
      maxConcurrentGet: Int = maxConcurrentGet,
      maxConcurrentRPC: Int = maxConcurrentRPC,
      maxConcurrentPublish: Int = maxConcurrentPublish
  ): ChannelConfig = new ChannelConfig(
    returnedBufSize = returnedBufSize,
    confirmBufSize = confirmBufSize,
    maxConcurrentGet = maxConcurrentGet,
    maxConcurrentRPC = maxConcurrentRPC,
    maxConcurrentPublish = maxConcurrentPublish
  )

  def withReturnedBufSize(size: Int): ChannelConfig =
    copy(returnedBufSize = size)
  def withConfirmedBufSize(size: Int): ChannelConfig =
    copy(confirmBufSize = size)
  def withMaxConcurrentGet(size: Int): ChannelConfig =
    copy(maxConcurrentGet = size)
  def withMaxConcurrentRPC(size: Int): ChannelConfig =
    copy(maxConcurrentRPC = size)
  def withMaxConcurrentPublish(size: Int): ChannelConfig =
    copy(maxConcurrentPublish = size)
}
object ChannelConfig {
  val default = new ChannelConfig()
}
final class ConnectionConfig private (
    val frameBufSize: Int = 100,
    val name: Option[ShortString] = None,
    val globalChannelConfig: ChannelConfig = ChannelConfig.default
) {
  private def copy(
      frameBufSize: Int = frameBufSize,
      name: Option[ShortString] = name,
      globalChannelConfig: ChannelConfig = globalChannelConfig
  ): ConnectionConfig =
    new ConnectionConfig(frameBufSize, name, globalChannelConfig)

  def withBufferSize(size: Int): ConnectionConfig = copy(frameBufSize = size)

  def withChannelConfig(config: ChannelConfig): ConnectionConfig =
    copy(globalChannelConfig = config)

  def withName(name: ShortString): ConnectionConfig =
    copy(name = Some(name))
  def noConnectionName: ConnectionConfig = copy(name = None)

  inline def withNameInline(name: String): ConnectionConfig =
    withName(ShortString(name))
}
object ConnectionConfig {
  val default: ConnectionConfig = new ConnectionConfig()
}
