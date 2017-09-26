import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import scalariform.formatter.preferences._

enablePlugins(JavaAppPackaging)
//coverageEnabled := true
val mySetting = Seq(
  organization := "com.souo",
  version := "0.0.1",
  scalaVersion := "2.11.8"
) ++ universalSettings

lazy val universalSettings = commonSettings ++ testSettings

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

lazy val commonSettings = scalariformSettings(autoformat = true) ++ Seq(
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(PreserveSpaceBeforeArguments, true)
    .setPreference(CompactControlReadability, true)
    .setPreference(AlignArguments, true)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(SpacesAroundMultiImports, false)
    .setPreference(RewriteArrowSymbols, true),

  compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle
    .in(Compile)
    .toTask("")
    .value,
  (compile in Compile) := ((compile in Compile) dependsOn compileScalastyle).value,
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-feature",
    "encode", "utf-8",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-language:existentials",
    "-language:higherKinds",
    "-Ywarn-dead-code")
)


lazy val testSettings = Seq(
  parallelExecution in Test := false,
  fork in Test := true,
  concurrentRestrictions in Global := Seq(
    Tags.limit(Tags.CPU, 1),
    Tags.limit(Tags.Test, 1),
    Tags.limitSum(1, Tags.Test, Tags.Untagged))
)

//enable our settings
mySetting
//library dependencies
libraryDependencies ++= Dependencies.designer
//libraryDependencies += "org.sangria-graphql" %% "sangria" % "1.2.1"

compile in Compile := {
  val compilationResult = (compile in Compile).value
  IO.touch(target.value / "compilationFinished")
  compilationResult
}

//define assembly jar name
assemblyJarName in assembly := "biplatform.jar"

//skip test
test in assembly := {}

//wartremoverErrors in (Compile, compile) ++= Warts.all
//wartremoverWarnings in (Compile, compile) ++= Warts.all

mappings in Universal := {
  // universalMappings: Seq[(File,String)]
  val universalMappings = (mappings in Universal).value
  val fatJar = (assembly in Compile).value
  // removing means filtering
  val filtered = universalMappings filter {
    case (file, name) => !name.endsWith(".jar")
  }
  //the fat jar
  filtered :+ (fatJar -> ("lib/" + fatJar.getName))
}


scriptClasspath := Seq((assemblyJarName in assembly).value)

//Skip packageDoc task on stage
mappings in(Compile, packageDoc) := Seq()


assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
  case PathList("com", "fasterxml", "jackson", xs@_*) => MergeStrategy.first
  case PathList("com", "google", "protobuf", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "calcite", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "commons", xs@_*) => MergeStrategy.first
  case PathList("org", "slf4j",  xs@_*) => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf" => MergeStrategy.concat
  case "unwanted.txt" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

addCommandAlias("zip", "universal:packageBin")
addCommandAlias("tgz", "universal:packageZipTarball")