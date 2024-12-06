import sbtcrossproject.CrossProject
import Dependencies.Versions
import sbt.ThisBuild

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val scala3 = "3.3.4"
val PrimaryJava = JavaSpec.temurin("8")
val LTSJava = JavaSpec.temurin("17")

inThisBuild(
  List(
    tlBaseVersion := "0.5",
    scalaVersion := scala3,
    fork := true,
    Test / fork := false,
    organization := "dev.hnaderi",
    organizationName := "Hossein Naderi",
    startYear := Some(2021),
    tlSonatypeUseLegacyHost := false,
    tlCiReleaseBranches := Seq("main"),
    tlSitePublishBranch := Some("main"),
    tlSiteJavaVersion := LTSJava,
    githubWorkflowJavaVersions := Seq(PrimaryJava, LTSJava),
    githubWorkflowBuildMatrixFailFast := Some(false),
    // This job is used as a sign that all build jobs have been successful and is used by mergify
    githubWorkflowAddedJobs += WorkflowJob(
      id = "post-build",
      name = "post build",
      needs = List("build"),
      steps = List(
        WorkflowStep.Run(
          commands = List("echo success!"),
          name = Some("post build")
        )
      ),
      scalas = Nil,
      javas = Nil
    ),
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

def module(mname: String): CrossProject => CrossProject =
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
    .jvmSettings(
      Test / fork := true
    )

val protocol = module("protocol") {
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .settings(
      libraryDependencies += "org.scodec" %%% "scodec-bits" % Versions.scodecBit
    )
}

val codeGen = Project(s"code-gen", file(s"modules/code-gen"))
  .settings(
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-io" % Versions.fs2,
      "co.fs2" %% "fs2-scodec" % Versions.fs2,
      "org.scala-lang.modules" %% "scala-xml" % Versions.xml
    ),
    Compile / run / baseDirectory := file("."),
    description := "Lepus internal code generator based on AMQP spec"
  )
  .enablePlugins(NoPublishPlugin)

val protocolTestkit = module("protocol-testkit") {
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

val wire = module("wire") {
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .dependsOn(protocol)
    .dependsOn(protocolTestkit % Test)
    .settings(
      libraryDependencies += "org.scodec" %%% "scodec-core" % Versions.scodec
    )
}

val client = module("client") {
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .dependsOn(wire, protocol)
    .dependsOn(protocolTestkit % Test)
    .enablePlugins(BuildInfoPlugin)
    .settings(
      buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion),
      buildInfoPackage := "lepus.client",
      buildInfoOptions ++= Seq(
        BuildInfoOption.ConstantValue,
        BuildInfoOption.PackagePrivate
      ),
      libraryDependencies ++=
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

val std = module("std") {
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .dependsOn(client)
    .settings(
      libraryDependencies += "dev.hnaderi" %%% "named-codec" % Versions.NamedCodec
    )
}

val circe = module("circe") {
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .dependsOn(client)
    .settings(
      libraryDependencies += "io.circe" %%% "circe-parser" % Versions.circe
    )
}

val example =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("example"))
    .dependsOn(std, circe)
    .enablePlugins(NoPublishPlugin)
    .settings(
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-generic" % Versions.circe,
        "dev.hnaderi" %%% "named-codec-circe" % Versions.NamedCodec
      )
    )
    .jvmSettings(
      fork := true
    )
    .jsSettings(
      scalaJSUseMainModuleInitializer := true,
      scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
    )
    .nativeSettings(
      libraryDependencies += "com.armanbilge" %%% "epollcat" % "0.1.6"
    )

val docs = project
  .in(file("site"))
  .enablePlugins(LepusSitePlugin)
  .disablePlugins(TypelevelSettingsPlugin)
  .dependsOn(example.jvm)

lazy val unidocs = project
  .in(file("unidocs"))
  .enablePlugins(TypelevelUnidocPlugin)
  .settings(
    name := "lepus-docs",
    description := "unified docs for lepus",
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
      client.jvm,
      std.jvm,
      protocol.jvm,
      wire.jvm
    )
  )

val root = tlCrossRootProject
  .settings(name := "lepus")
  .aggregate(
    protocol,
    protocolTestkit,
    wire,
    codeGen,
    client,
    std,
    circe,
    docs,
    unidocs,
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
