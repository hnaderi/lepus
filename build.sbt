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

val client = Module
  .named("client")
  .dependsOn(core)
  .settings(libraryDependencies ++= rabbit)

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
    core,
    client,
    docs
  )
  .enablePlugins(MicrositesPlugin)
