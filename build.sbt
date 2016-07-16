import scalariform.formatter.preferences._

version := VersionHelper.versionByTag

resolvers in ThisBuild ++= Seq(
  Resolver.typesafeRepo("releases"),
  Resolver.jcenterRepo
)

libraryDependencies ++= {

  val akka =
    "com.typesafe.akka" %% "akka-actor" % "2.4.8" ::
      "com.typesafe.akka" %% "akka-agent" % "2.4.8" ::
    "com.typesafe.akka" %% "akka-http-core" % "2.4.8" ::
    "com.typesafe.akka" %% "akka-http-experimental" % "2.4.8" ::
    ("com.typesafe.akka" %% "akka-slf4j" % "2.4.8" exclude("org.slf4j", "slf4j-api")) :: Nil

  val json = "org.json4s" %% "json4s-native" % "3.4.0" :: Nil

  val config = "com.typesafe" % "config" % "1.3.0" :: Nil

  val logging =
    "org.slf4j" % "slf4j-api" % "1.7.21" ::
    ("ch.qos.logback" % "logback-classic" % "1.1.7" exclude("org.slf4j", "slf4j-api")) ::
    ("com.typesafe.scala-logging" %% "scala-logging" % "3.4.0" exclude("org.slf4j", "slf4j-api")) :: Nil

  akka ++ json ++ config ++ logging
}

scalariformSettings ++ Seq(ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)
  .setPreference(RewriteArrowSymbols, true))

scalaVersion := "2.11.8"

scalacOptions += "-target:jvm-1.8"

javacOptions ++= Seq("-encoding", "UTF-8")

scalacOptions in ThisBuild ++= Seq(Opts.compile.deprecation, Opts.compile.unchecked) ++
  Seq("-Ywarn-unused-import", "-Ywarn-unused", "-Xlint", "-feature")
