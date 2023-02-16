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

package lepus.std

import lepus.protocol.domains.*

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

private object ShortStringConstructors {

  def md5Hex(str: String): ShortString = hexHash(str, "MD5")
  def sha1Hex(str: String): ShortString = hexHash(str, "SHA-1")
  def sha224Hex(str: String): ShortString = hexHash(str, "SHA-224")
  def sha256Hex(str: String): ShortString = hexHash(str, "SHA-256")
  def sha384Hex(str: String): ShortString = hexHash(str, "SHA-384")
  def sha512Hex(str: String): ShortString = hexHash(str, "SHA-512")

  private def hexHash(str: String, algorithm: String): ShortString = {
    val digest = MessageDigest.getInstance(algorithm);
    digest.update(str.getBytes(StandardCharsets.UTF_8))

    val out = digest.digest()

    ShortString.unsafe(Base64.getEncoder().encodeToString(out))
  }
}

extension (o: ShortString.type) {
  inline def md5Hex(str: String): ShortString =
    ShortStringConstructors.md5Hex(str)
  inline def sha1Hex(str: String): ShortString =
    ShortStringConstructors.sha1Hex(str)
  inline def sha224Hex(str: String): ShortString =
    ShortStringConstructors.sha224Hex(str)
  inline def sha256Hex(str: String): ShortString =
    ShortStringConstructors.sha256Hex(str)
  inline def sha384Hex(str: String): ShortString =
    ShortStringConstructors.sha384Hex(str)
  inline def sha512Hex(str: String): ShortString =
    ShortStringConstructors.sha512Hex(str)
}

extension (str: String) {
  inline def md5Hex: ShortString =
    ShortStringConstructors.md5Hex(str)
  inline def sha1Hex: ShortString =
    ShortStringConstructors.sha1Hex(str)
  inline def sha224Hex: ShortString =
    ShortStringConstructors.sha224Hex(str)
  inline def sha256Hex: ShortString =
    ShortStringConstructors.sha256Hex(str)
  inline def sha384Hex: ShortString =
    ShortStringConstructors.sha384Hex(str)
  inline def sha512Hex: ShortString =
    ShortStringConstructors.sha512Hex(str)
}
