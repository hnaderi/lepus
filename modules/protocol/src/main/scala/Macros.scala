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

package lepus.protocol.domains

import scala.quoted.*

private abstract class Literally[T, R](using FromExpr[T], ToExpr[R]) {
  private def validate(t: T)(using Quotes): Either[String, Expr[R]] =
    from(t).map(Expr(_))

  protected final def build(t: Expr[T])(using ctx: Quotes): Expr[R] =
    import ctx.reflect.report
    t.value match {
      case Some(v) => validate(v).fold(report.errorAndAbort, identity)
      case None    => report.errorAndAbort("Not a literal value!")
    }

  def from(str: T): Either[String, R]
}
