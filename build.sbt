import Dependencies.Libraries._
import sbt.ThisBuild

name := "lepus"
Global / onChangedBuildSource := ReloadOnSourceChanges

val libVersion = "1.0.0-SNAPSHOT"

version := libVersion

lazy val scala3 = "3.0.1"
ThisBuild / scalaVersion := scala3
ThisBuild / version := libVersion

val core = Module
  .named("core")
  .settings(libraryDependencies ++= cats ++ catsEffect ++ fs2)

val protocol = Module
  .named("protocol")

val protocolGen = Module
  .named("protocol-gen")
  .settings(libraryDependencies ++= fs2IO ++ scodecStream ++ scalaXml)

val data = Module
  .named("data")
  .dependsOn(core)

val client = Module
  .named("client")
  .dependsOn(core)
  .dependsOn(protocol)
  .settings(
    libraryDependencies ++= rabbit ++ scodec ++ fs2IO ++ scodecStream ++ scalaXml
  )

val std = Module
  .named("std")
  .dependsOn(core)
  .dependsOn(data)

val dataCirce = Module
  .named("data-circe")
  .dependsOn(data)
  .settings(libraryDependencies ++= circe)

val docs = project
  .in(file("docs-build"))
  .dependsOn(core)
  .enablePlugins(MdocPlugin)

mdocVariables := Map(
  "VERSION" -> version.value
)

val root = (project in file("."))
  .settings(
    Common.settings,
    version := libVersion
  )
  .aggregate(
    protocol,
    protocolGen,
    core,
    client,
    std,
    data,
    dataCirce,
    docs
  )
  .enablePlugins(MicrositesPlugin)
