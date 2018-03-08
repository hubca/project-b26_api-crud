import play.sbt.routes.RoutesKeys
//import com.typesafe.sbt.less.Import.LessKeys
//import com.typesafe.sbt.web.Import.Assets
import sbt.Keys.libraryDependencies


// todo - check 2.11.8 for integration with Spark?
name := "project-b26"

val moreLibDependencies = Seq(guice, ws)

lazy val admin = (project in file("modules/admin")).enablePlugins(PlayScala).settings(
  Common.settings,
  routesImport ++= Seq("services.CustomBinders._", "reactivemongo.bson.BSONObjectID"),
  libraryDependencies ++= moreLibDependencies
)

lazy val root = (project in file("."))
  .settings(
    Common.settings,
    libraryDependencies ++= moreLibDependencies
  )
  .enablePlugins(PlayScala).dependsOn(admin).aggregate(admin)

//includeFilter in (Assets, LessKeys.less) := "*.less"
//excludeFilter in (Assets, LessKeys.less) := "_*.less"
//LessKeys.compress in Assets := false

//excludeFilter in (Assets, LessKeys.less) := "_*.less"
//includeFilter in (Assets, LessKeys.less) := "admin1.less"

//routesGenerator := InjectedRoutesGenerator
//RoutesKeys.routesImport += "play.modules.reactivemongo.PathBindables._"

//libraryDependencies += guice
//libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test
//libraryDependencies += "com.h2database" % "h2" % "1.4.194"
//libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"
//libraryDependencies += ws

// only for Play 2.6.x
//libraryDependencies ++= Seq(
//  "org.reactivemongo" %% "play2-reactivemongo" % "0.12.5-play26"
//)

//resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
//scalacOptions in ThisBuild ++= Seq("-feature", "-language:postfixOps")

//libraryDependencies += "org.webjars" % "bootstrap" % "3.1.1-2"

//TwirlKeys.templateFormats += ("streamit" -> "uit.HtmlStreamFormat1")
//TwirlKeys.templateImports ++= Vector("uit.HtmlStream1", "uit.HtmlStream1._", "uit.HtmlStreamFormat1._", "uit.HtmlStreamImplicits1._")
