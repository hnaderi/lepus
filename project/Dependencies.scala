import sbt.CrossVersion
import sbt._

object Dependencies {

  object Versions {
    // val odin = "0.12.0"
    val cats = "2.6.1"
    val catsEffect = "3.2.2"
    val fs2 = "3.1.0"
    val scodec = "2.0.0"
    val scodecStream = "3.0.1"
    val circe = "0.14.1"
    val rabbit = "5.13.0"
    val MUnit = "0.7.28"
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

    val circe: Seq[ModuleID] = Seq(
      "io.circe" %% "circe-core"
    ).map(_ % Versions.circe)

    val rabbit: Seq[ModuleID] = Seq(
      "com.rabbitmq" % "amqp-client" % Versions.rabbit
    )

    val scodec = Seq(
      "org.scodec" %% "scodec-core" % Versions.scodec
    )

    val scodecStream = Seq(
      "org.scodec" %% "scodec-stream" % Versions.scodecStream
    )

    val scalaXml = Seq(
      "org.scala-lang.modules" %% "scala-xml" % "2.0.1"
    )

    // val odin: Seq[ModuleID] = Seq(
    //   "com.github.valskalla" %% "odin-core",
    //   "com.github.valskalla" %% "odin-slf4j"
    // ).map(_ % Versions.odin)

    val munit = Seq(
      "org.scalameta" %% "munit" % Versions.MUnit % Test,
      "org.scalameta" %% "munit-scalacheck" % "0.7.27" % Test,
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.5" % Test
    )
  }
}
