package lepus.codecs

import org.scalacheck.Prop
import scodec.Attempt

trait CodecTest extends munit.ScalaCheckSuite {
  protected given (Attempt[Unit] => Prop) = a =>
    a.map(_ => Prop(true)).getOrElse(Prop(false))
}
