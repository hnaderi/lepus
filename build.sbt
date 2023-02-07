import sbtcrossproject.CrossProject
import Dependencies.Versions
import sbt.ThisBuild

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val scala3 = "3.2.2"
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
}

def module2(mname: String): CrossProject => CrossProject =
  _.in(file(s"modules/$mname"))
    .settings(
      name := s"lepus-$mname",
      libraryDependencies ++= Seq(
        "org.scalameta" %%% "munit" % Versions.MUnit % Test,
        "org.scalameta" %%% "munit-scalacheck" % Versions.MUnit % Test,
        "org.typelevel" %%% "munit-cats-effect" % Versions.CatsEffectMunit % Test,
        "org.typelevel" %%% "scalacheck-effect-munit" % Versions.scalacheckEffectVersion % Test,
        "org.typelevel" %%% "cats-effect-testkit" % Versions.catsEffect % Test
      )
    )

val protocol = module2("protocol") {
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .settings(
      libraryDependencies += "org.scodec" %%% "scodec-bits" % "1.1.35"
    )
}

val codeGen = module("code-gen")
  .settings(
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-io" % Versions.fs2,
      "co.fs2" %% "fs2-scodec" % Versions.fs2,
      "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
    ),
    Compile / run / baseDirectory := file("."),
    description := "Lepus internal code generator based on AMQP spec"
  )
  .enablePlugins(NoPublishPlugin)

val protocolTestkit = module2("protocol-testkit") {
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .dependsOn(protocol)
    .settings(
      libraryDependencies ++= Seq(
        "org.scalameta" %%% "munit" % Versions.MUnit,
        "org.scalameta" %%% "munit-scalacheck" % Versions.MUnit,
        "org.typelevel" %%% "munit-cats-effect" % Versions.CatsEffectMunit
      )
    )
}

val wire = module2("wire") {
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .dependsOn(protocol)
    .dependsOn(protocolTestkit % Test)
    .settings(
      libraryDependencies += "org.scodec" %%% "scodec-core" % Versions.scodec
    )
}

val core = module2("core") {
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .settings(
      libraryDependencies ++= Seq(
        "org.typelevel" %%% "cats-core" % Versions.cats,
        "org.typelevel" %%% "cats-effect" % Versions.catsEffect,
        "co.fs2" %%% "fs2-core" % Versions.fs2
      )
    )
    .dependsOn(protocol)
}

val data = module2("data") {
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .dependsOn(core)
}

val client = module2("client") {
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .dependsOn(core, wire, protocol)
    .dependsOn(protocolTestkit % Test)
    .settings(
      libraryDependencies ++= // rabbit ++
        Seq(
          "co.fs2" %%% "fs2-io" % Versions.fs2,
          "co.fs2" %%% "fs2-scodec" % Versions.fs2,
          "org.typelevel" %%% "scalacheck-effect-munit" % Versions.scalacheckEffectVersion % Test,
          "org.typelevel" %%% "cats-effect-testkit" % Versions.catsEffect % Test
        )
    )
    .jvmSettings(
      libraryDependencies += "com.rabbitmq" % "amqp-client" % Versions.rabbit % Test
    )
}

val std = module2("std") {
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .dependsOn(client)
    .dependsOn(data)
}

val example =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("example"))
    .dependsOn(client)
    .enablePlugins(NoPublishPlugin)
    .jvmSettings(
      fork := true
    )
    .nativeSettings(
      libraryDependencies += "com.armanbilge" %%% "epollcat" % "0.1.3"
    )

val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(core.jvm)
  .settings(
    tlSiteRelatedProjects := Seq(
      TypelevelProject.Cats,
      TypelevelProject.CatsEffect,
      TypelevelProject.Fs2
    ),
    tlSiteHeliumConfig := SiteConfigs(mdocVariables.value)
  )

val root = tlCrossRootProject
  .settings(name := "lepus")
  .aggregate(
    protocol,
    protocolTestkit,
    wire,
    codeGen,
    core,
    client,
    std,
    data,
    docs,
    example
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
