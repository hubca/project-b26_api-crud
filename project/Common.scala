import sbt._
import Keys._
import play.sbt.routes.RoutesKeys
import play.sbt.routes.RoutesKeys._
import play.twirl.sbt.Import.TwirlKeys

object Common {

  val commonResolvers = Seq(
    Resolver.sonatypeRepo("snapshots"),
    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
  )

  val commonLibDependencies = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test,
    "com.h2database" % "h2" % "1.4.194",
    "org.webjars" % "metisMenu" % "1.1.3",
    "com.typesafe.play" %% "play-json" % "2.6.0",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.13.0-play26",
    "org.webjars.bower" % "bootstrap-sass" % "3.3.6",
    "org.webjars" % "bootstrap" % "3.3.4",
    "org.webjars" % "font-awesome" % "4.7.0",
    //"org.webjars" % "jquery" % "2.1.3",
    "org.webjars" % "datatables" % "1.10.5",
    "org.webjars" % "datatables-plugins" % "1.10.5"
  )

  val settings: Seq[Setting[_]] = Seq(
    organization := "com.boardrs",
    version := "1.0-SNAPSHOT",
    routesGenerator := InjectedRoutesGenerator,
    RoutesKeys.routesImport += "play.modules.reactivemongo.PathBindables._",
    scalaVersion := "2.12.2",
    scalacOptions ++= Seq("-feature", "-language:postfixOps"),
    resolvers ++= commonResolvers,
    libraryDependencies ++= commonLibDependencies,
    TwirlKeys.templateFormats += ("stream" -> "ui.HtmlStreamFormat"),
    TwirlKeys.templateImports ++= Vector("ui.HtmlStream", "ui.HtmlStream._", "ui.HtmlStreamFormat._", "ui.HtmlStreamImplicits._")
  )

}