import Dependencies.Libraries._
import Dependencies.Versions
import sbt.ThisBuild

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val scala3 = "3.1.2"
val PrimaryJava = JavaSpec.temurin("8")
val LTSJava = JavaSpec.temurin("17")

inThisBuild(
  List(
    tlBaseVersion := "0.0",
    scalaVersion := scala3,
    fork := true,
    Test / fork := false,
    organization := "dev.hnaderi",
    organizationName := "Hossein Naderi",
    startYear := Some(2021),
    tlSonatypeUseLegacyHost := false,
    tlCiReleaseBranches := Seq(), // No snapshots while not ready!
    tlSitePublishBranch := Some("main"),
    githubWorkflowJavaVersions := Seq(PrimaryJava, LTSJava),
    licenses := Seq(License.Apache2),
    developers := List(
      Developer(
        id = "hnaderi",
        name = "Hossein Naderi",
        email = "mail@hnaderi.dev",
        url = url("https://hnaderi.dev")
      )
    )
  )
)

def module(module: String): Project = {
  val id = s"lepus-$module"
  Project(id, file(s"modules/$module"))
    .settings(
      libraryDependencies ++= (munit)
    )
}

val protocol = module("protocol")

val protocolGen = module("protocol-gen")
  .settings(
    libraryDependencies ++= fs2IO ++ fs2scodec ++ scalaXml,
    Compile / run / baseDirectory := file(".")
  )

val protocolTestkit = module("protocol-testkit")
  .dependsOn(protocol)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % Versions.MUnit,
      "org.scalameta" %% "munit-scalacheck" % "0.7.27",
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.5"
    )
  )

val wire = module("wire")
  .dependsOn(protocol)
  .dependsOn(protocolTestkit % Test)
  .settings(libraryDependencies ++= scodec)

val core = module("core")
  .settings(libraryDependencies ++= cats ++ catsEffect ++ fs2)
  .dependsOn(protocol)

val data = module("data")
  .dependsOn(core)

val client = module("client")
  .dependsOn(core, wire, protocol)
  .dependsOn(protocolTestkit % Test)
  .settings(
    libraryDependencies ++= rabbit ++ scodec ++ fs2IO ++ fs2scodec
  )

val std = module("std")
  .dependsOn(client)
  .dependsOn(data)

val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(core)
  .settings(
    tlSiteRelatedProjects := Seq(
      TypelevelProject.Cats,
      TypelevelProject.CatsEffect,
      TypelevelProject.Fs2
    ),
    tlSiteHeliumConfig := SiteConfigs(mdocVariables.value)
  )

val root = project
  .in(file("."))
  .settings(
    name := "lepus"
  )
  .aggregate(
    protocol,
    protocolTestkit,
    wire,
    protocolGen,
    core,
    client,
    std,
    data,
    docs
  )

def addAlias(name: String)(tasks: String*) =
  addCommandAlias(name, tasks.mkString(" ;"))

addAlias("commit")(
  "clean",
  "scalafmtCheckAll",
  "scalafmtSbtCheck",
  "headerCheckAll",
  "githubWorkflowCheck",
  "compile",
  "test"
)
addAlias("precommit")(
  "scalafmtAll",
  "scalafmtSbt",
  "headerCreateAll",
  "githubWorkflowGenerate",
  "compile",
  "test"
)
