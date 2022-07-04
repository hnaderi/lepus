import sbt.CrossVersion
import sbt._

object Dependencies {

  object Versions {
    val cats = "2.8.0"
    val catsEffect = "3.3.13"
    val fs2 = "3.2.9"
    val scodec = "2.1.0"
    val scodecStream = "3.0.1"
    val scodecBit = "1.1.13"
    val circe = "0.14.1"
    val rabbit = "5.13.0"
    val MUnit = "0.7.29"
    val scalacheckEffectVersion = "1.0.4"
  }

  object Libraries {
    val cats: Seq[ModuleID] = Seq(
      "org.typelevel" %% "cats-core"
    ).map(_ % Versions.cats)

    val catsEffect: Seq[ModuleID] = Seq(
      "org.typelevel" %% "cats-effect" % Versions.catsEffect
    )

    val fs2: Seq[ModuleID] = Seq(
      "co.fs2" %% "fs2-core" % Versions.fs2
    )

    val fs2IO: Seq[ModuleID] = Seq(
      "co.fs2" %% "fs2-io" % Versions.fs2
    )

    val fs2scodec: Seq[ModuleID] = Seq(
      "co.fs2" %% "fs2-scodec" % Versions.fs2
    )

    val circe: Seq[ModuleID] = Seq(
      "io.circe" %% "circe-core"
    ).map(_ % Versions.circe)

    val rabbit: Seq[ModuleID] = Seq(
      "com.rabbitmq" % "amqp-client" % Versions.rabbit % Test
    )

    val scodec = Seq(
      "org.scodec" %% "scodec-core" % Versions.scodec
    )

    val scalaXml = Seq(
      "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
    )

    val munit = Seq(
      "org.scalameta" %% "munit" % Versions.MUnit % Test,
      "org.scalameta" %% "munit-scalacheck" % "0.7.29" % Test,
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
    )
  }
}
