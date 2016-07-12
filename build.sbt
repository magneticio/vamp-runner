import sbt.ExclusionRule

import scalariform.formatter.preferences._

version := VersionHelper.versionByTag

resolvers in ThisBuild ++= Seq(
  Resolver.typesafeRepo("releases"),
  Resolver.jcenterRepo
)

libraryDependencies ++= {

  val akka = "2.4.8"
  val json4s = "3.3.0"
  val config = "1.3.0"
  val logging = "3.1.0"
  val slf4j = "1.7.16"
  val logback = "1.1.5"

  Seq(
    "com.typesafe.akka" %% "akka-actor" % akka,
    "com.typesafe.akka" %% "akka-stream" % akka excludeAll ExclusionRule(organization = "com.typesafe"),
    "com.typesafe.akka" %% "akka-http-core" % akka,
    "com.typesafe.akka" %% "akka-slf4j" % akka excludeAll ExclusionRule(organization = "org.slf4j"),
    "org.json4s" %% "json4s-native" % json4s,
    "com.typesafe" % "config" % config,
    "com.typesafe.scala-logging" %% "scala-logging" % logging excludeAll ExclusionRule(organization = "org.slf4j"),
    "org.slf4j" % "slf4j-api" % slf4j,
    "ch.qos.logback" % "logback-classic" % logback
  )
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
