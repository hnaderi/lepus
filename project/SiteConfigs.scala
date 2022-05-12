import laika.ast.Path.Root
import laika.ast._
import laika.config.ConfigBuilder
import laika.config.LaikaKeys
import laika.helium.Helium
import laika.helium.config._
import laika.sbt.LaikaConfig
import laika.theme._
import laika.theme.config.Color

object SiteConfigs {
  def apply(vars: Map[String, String]): Helium = Helium.defaults.site
    .metadata(
      title = Some("Lepus"),
      authors = Seq("Hossein Naderi"),
      language = Some("en")
    )
    .site
    .favIcons(
      Favicon.internal(Root / "icon.png", "32x32")
    )
    .site
    .landingPage(
      logo = Some(
        Image.internal(
          Root / "icon.png",
          width = Some(Length(50, LengthUnit.percent))
        )
      ),
      title = Some("Lepus"),
      subtitle = Some("Purely functional client for RabbitMQ"),
      latestReleases = Seq(
        ReleaseInfo(
          "Latest develop Release",
          vars.getOrElse("SNAPSHOT_VERSION", "N/A")
        ),
        ReleaseInfo("Latest Stable Release", vars.getOrElse("VERSION", "N/A"))
      ),
      license = Some("Apache 2.0"),
      documentationLinks = Seq(
        TextLink.internal(Root / "introduction.md", "Inroduction")
      ),
      teasers = Seq(
        Teaser(
          "Purely functional",
          "Fully referentially transparent, no exceptions or runtime reflection and integration with cats-effect for polymorphic effect handling."
        ),
        Teaser(
          "Pure scala",
          "Implemented from scratch in scala, to improve ergonomics and integration, and also javascript support"
        ),
        Teaser(
          "Non-blocking streaming",
          "Built on top of fs2 streaming, no blocking, no waste."
        )
      )
    )
    .site
    .topNavigationBar(
      homeLink = ImageLink
        .internal(Root / "introduction.md", Image.internal(Root / "icon.png")),
      navLinks = Seq(
        IconLink
          .external("https://lepus.hnaderi.dev/", HeliumIcon.github)
      )
    )
    .site
    .baseURL("https://lepus.hnaderi.dev/")

}
