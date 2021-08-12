import sbt._
import sbt.Keys._
import Dependencies._

object Module {

  def named(module: String): Project = {
    val id = s"$module"
    Project(id, file(s"modules/$module"))
      .settings(
        Common.settings,
        libraryDependencies ++= (
          Libraries.munit
        )
      )
  }

}
