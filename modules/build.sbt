import play.sbt.routes.RoutesKeys
//import com.typesafe.sbt.less.Import.LessKeys
//import com.typesafe.sbt.web.Import.Assets
import sbt.Keys.includeFilter

name := "admin"

//enablePlugins(PlayScala)
//enablePlugins(SbtWeb)


//includeFilter in (Assets, LessKeys.less) := "*.less"

PlayKeys.devSettings += ("play.http.router", "admin.Routes")
//routesImport ++= Seq("services.CustomBinders._", "reactivemongo.bson.BSONObjectID")
//Common.settings
//libraryDependencies += Common.fooDependency
//libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"

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

