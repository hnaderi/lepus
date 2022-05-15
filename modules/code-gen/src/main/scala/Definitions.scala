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

package lepus.protocol.gen

enum PrimitiveType {
  case bit, octet, short, long, longlong, shortstr, longstr, timestamp, table
}

final case class Domain(
    name: String,
    dataType: PrimitiveType,
    label: String,
    doc: Option[String],
    assertions: List[String]
)

final case class Class(
    name: String,
    id: Short,
    label: String,
    doc: String,
    methods: List[Method]
)

enum MethodType {
  case Sync, ASync
}

enum MethodReceiver {
  case Server, Client, Both
}

final case class Method(
    name: String,
    id: Short,
    label: String,
    sync: MethodType,
    receiver: MethodReceiver,
    doc: String,
    fields: List[Field],
    responses: List[String]
)

final case class Field(
    name: String,
    label: String,
    dataType: String,
    doc: String,
    reserved: Boolean
)
