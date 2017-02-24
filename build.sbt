import java.text.SimpleDateFormat
import java.util.Date
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scala.util.Try
import scalariform.formatter.preferences._
import NativePackagerHelper._

enablePlugins(JavaAppPackaging)

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
  "-unchecked",
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

mySetting

libraryDependencies ++= Dependencies.designer

buildInfoKeys := Seq[BuildInfoKey](
  BuildInfoKey.action("buildDate")(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())),
  BuildInfoKey.action("buildSha")(Try(Process("git rev-parse HEAD").!!.stripLineEnd).getOrElse("?"))
)


compile in Compile := {
  val compilationResult = (compile in Compile).value
  IO.touch(target.value / "compilationFinished")
  compilationResult
}

assemblyJarName in assembly := "biplatform.jar"

//skip test
test in assembly := {}

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

//add our script
mappings in Universal ++= {
  contentOf("script").map{s => s._1 -> ("bin/" + s._2)}
}
//需要的话  打包jdk1.8
//mappings in Universal ++= directory("data/jdk1.8.0_121")

scriptClasspath := Seq((assemblyJarName in assembly).value)

//Skip packageDoc task on stage
mappings in (Compile, packageDoc) := Seq()


lazy val zip = taskKey[Unit]("Packaging zip  file and move it to deploy ")

zip := {
  val from = (packageBin in Universal).value
  val to = baseDirectory.value / "deploy" / from.getName
  if (to.delete()){
    println(s"delete old file ${to.getName}")
  }
  IO.move(from, to)
}


lazy val tgz = taskKey[Unit]("Packaging  tgz  file and move it  deploy ")

tgz := {
  val from = (packageZipTarball in Universal).value
  val to = baseDirectory.value / "deploy" / from.getName
  if (to.delete()){
    println(s"delete old file ${to.getName}")
  }
  IO.move(from, to)
}



