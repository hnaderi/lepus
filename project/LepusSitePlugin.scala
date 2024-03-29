import laika.ast.Path.Root
import laika.ast._
import laika.config.LaikaKeys
import laika.helium.Helium
import laika.helium.config._
import laika.theme._
import laika.theme.config.Color
import org.typelevel.sbt.TypelevelSitePlugin
import mdoc.MdocPlugin.autoImport.mdocVariables
import org.typelevel.sbt.TypelevelSitePlugin.autoImport._
import org.typelevel.sbt.TypelevelVersioningPlugin.autoImport._
import laika.sbt.LaikaPlugin.autoImport._
import sbt._
import sbt.Keys._
import laika.config.LinkConfig
import laika.config.SourceLinks
import laika.config.ApiLinks
import laika.format.Markdown
import laika.config.SyntaxHighlighting

object LepusSitePlugin extends AutoPlugin {
  override def requires: Plugins = TypelevelSitePlugin

  private def tl(repo: String) =
    TextLink.external(s"https://typelevel.org/$repo/", repo)

  private val relatedProjectLinks = ThemeNavigationSection(
    "Related projects",
    tl("cats"),
    tl("cats-effect"),
    TextLink.external("https://fs2.io/", "fs2"),
    TextLink.external("https://github.com/scodec/scodec/", "scodec")
  )

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    tlSiteHelium := {
      Helium.defaults.site
        .themeColors(
          primary = Color.hex("ceeaeb"),
          primaryMedium = Color.hex("ceeaeb"),
          secondary = Color.hex("f2c12e"),
          primaryLight = Color.hex("0d1826"),
          text = Color.hex("c2ccff"),
          background = Color.hex("0d1826"),
          bgGradient = Color.hex("0420bf") -> Color.hex("3113f2")
        )
        .site
        .internalCSS(Root / "styles")
        .site
        .metadata(
          title = Some("Lepus"),
          authors = Seq("Hossein Naderi"),
          language = Some("en"),
          version = Some(version.value)
        )
        .site
        .favIcons(
          Favicon.internal(Root / "lepus-transparent.png", "32x32")
        )
        .site
        .landingPage(
          logo = Some(
            Image.internal(
              Root / "lepus-transparent.png",
              width = Some(Length(100, LengthUnit.percent))
            )
          ),
          title = Some("Lepus"),
          subtitle = Some("Purely functional client for RabbitMQ"),
          latestReleases = Seq(
            ReleaseInfo(
              "Latest develop Release",
              version.value
            ),
            ReleaseInfo(
              "Latest Stable Release",
              tlLatestVersion.value.getOrElse("N/A")
            )
          ),
          license = licenses.value.headOption.map(_._1),
          documentationLinks = Seq(
            TextLink.internal(Root / "getting-started.md", "Getting Started"),
            TextLink.internal(Root / "standard-library.md", "Standard library"),
            TextLink.internal(Root / "features.md", "Features"),
            TextLink.internal(Root / "examples" / "README.md", "Examples")
          ) ++ tlSiteApiUrl.value
            .map(_.toString())
            .map(TextLink.external(_, "API docs")),
          projectLinks = Seq(
            IconLink.external(
              "https://github.com/hnaderi/lepus",
              HeliumIcon.github
            )
          ),
          teasers = Seq(
            Teaser(
              "Non-blocking streaming",
              "Built on top of fs2 streaming, no blocking, no waste."
            ),
            Teaser(
              "AMQP compliant",
              "Implements AMQP 0.9.1 and RabbitMQ protocol extensions, supports all of the RabbitMQ features."
            ),
            Teaser(
              "Purely functional",
              "Fully referential transparent, no exceptions or runtime reflection and integration with cats-effect for polymorphic effect handling."
            ),
            Teaser(
              "Pure scala",
              "Implemented from scratch in scala, to improve ergonomics and integration"
            ),
            Teaser(
              "Cross platform",
              "Supports all scala platforms, JVM, JS and Native"
            )
          )
        )
        .site
        .topNavigationBar(
          homeLink = ImageLink
            .internal(
              Root / "getting-started.md",
              Image.internal(Root / "lepus-transparent.png")
            ),
          navLinks = scmInfo.value.toSeq.map(repo =>
            IconLink.external(
              repo.browseUrl.toString(),
              HeliumIcon.github
            )
          )
        )
        .site
        .mainNavigation(appendLinks = Seq(relatedProjectLinks))
        .site
        .baseURL("https://lepus.hnaderi.dev/")
        .site
        .darkMode
        .disabled

    },
    laikaConfig := {
      val apiDoc = tlSiteApiUrl.value.toSeq
        .map(_.toString())
        .map(ApiLinks(_).withPackagePrefix("lepus"))
      val repo = scmInfo.value.toSeq
        .map(_.browseUrl.toString())
        .map(url => s"$url/tree/main/example/src/main/scala/")
        .map(SourceLinks(_, "scala"))

      LaikaConfig.defaults.withConfigValue(
        LinkConfig.empty.addApiLinks(apiDoc: _*).addSourceLinks(repo: _*)
      )
    },
    laikaIncludeAPI := true,
    laikaInputs ~= {
      _.delegate
        .addDirectory("example/src/main/scala/example", Root / "examples")
        .addDirectory("example/.jvm/src/main/scala/example", Root / "examples")
    },
    laikaExtensions := Seq(Markdown.GitHubFlavor, SyntaxHighlighting)
  )
}
