import java.text.SimpleDateFormat
import java.util.Date
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scala.util.Try
import scalariform.formatter.preferences._

val mySetting = Seq(
  organization := "com.souo",
  version := "0.0.1",
  scalaVersion := "2.11.8"
) ++ universalSettings

lazy val universalSettings = commonSettings ++ testSettings

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

lazy val commonSettings = SbtScalariform.scalariformSettings ++ Seq(
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(PreserveSpaceBeforeArguments, true)
    .setPreference(CompactControlReadability, true)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(SpacesAroundMultiImports, false)
    .setPreference(AlignParameters, true)
    .setPreference(AlignArguments, true)
    .setPreference(RewriteArrowSymbols, true),

compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle
  .in(Compile)
  .toTask("")
  .value,
(compile in Compile) := ((compile in Compile) dependsOn compileScalastyle).value,
scalacOptions ++= Seq(
  "-deprecation",
  "unchecked",
  "-feature",
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

lazy val biplatfrom = (project in file("."))
  .settings(mySetting: _*)
  .settings(
    libraryDependencies ++= Dependencies.designer,
    buildInfoKeys := Seq[BuildInfoKey](
      BuildInfoKey.action("buildDate")(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())),
      BuildInfoKey.action("buildSha")(Try(Process("git rev-parse HEAD").!!.stripLineEnd).getOrElse("?"))),
    compile in Compile := {
      val compilationResult = (compile in Compile).value
      IO.touch(target.value / "compilationFinished")
      compilationResult
    },
    mainClass in Compile := Some("com.souo.biplatform.system.LocalMain"),
    assemblyJarName in assembly := "biplatform.jar"
  )


