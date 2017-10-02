import play.sbt.routes.RoutesKeys

name := """project-b26"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.12.2"

routesGenerator := InjectedRoutesGenerator
RoutesKeys.routesImport += "play.modules.reactivemongo.PathBindables._"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test
libraryDependencies += "com.h2database" % "h2" % "1.4.194"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"
libraryDependencies += ws

// only for Play 2.6.x
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.12.5-play26"
)

//resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
//scalacOptions in ThisBuild ++= Seq("-feature", "-language:postfixOps")

libraryDependencies += "org.webjars" % "bootstrap" % "3.1.1-2"

TwirlKeys.templateFormats += ("stream" -> "ui.HtmlStreamFormat")
TwirlKeys.templateImports ++= Vector("ui.HtmlStream", "ui.HtmlStream._", "ui.HtmlStreamFormat._", "ui.HtmlStreamImplicits._")