import sbt._

object Version {
  val slf4jVersion = "1.7.21"
  val scalaLoggingVersion = "3.1.0"
  val slickVersion = "3.1.1"
  val circeVersion = "0.6.1"
  val akkaV = "2.4.14"
  val akkaHttpV = "10.0.0"
  val catsV = "0.8.1"
}

object Libary {

  import Version._

  val cats = "org.typelevel" %% "cats" % catsV

//  val elastic4sCore=  "com.sksamuel.elastic4s"  %% "elastic4s-core" % "1.7.0"

  val guava = "com.google.guava" % "guava" % "18.0"
  val commonsIo = "commons-io" % "commons-io" % "2.4"
  val slf4jApi = "org.slf4j" % "slf4j-api" % slf4jVersion
  val slf4jLog4j12 = "org.slf4j" % "slf4j-log4j12" % slf4jVersion
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion
  val loggingStack = Seq(slf4jApi, slf4jLog4j12, scalaLogging)

  val typesafeConfig = "com.typesafe" % "config" % "1.3.0"

  val jodaTime = "joda-time" % "joda-time" % "2.8.2"
  val jodaConvert = "org.joda" % "joda-convert" % "1.7"

  val circeCore = "io.circe" %% "circe-core" % circeVersion
  val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  val circeJawn = "io.circe" %% "circe-jawn" % circeVersion
  val circe = Seq(circeCore, circeGeneric, circeJawn)

  val javaxMailSun = "com.sun.mail" % "javax.mail" % "1.5.5"

  val slick = "com.typesafe.slick" %% "slick" % slickVersion
  val slickHikari = ("com.typesafe.slick" %% "slick-hikaricp" % slickVersion)
    .exclude("com.zaxxer", "HikariCP-java6")
  val h2 = "com.h2database" % "h2" % "1.4.193"
  val mysqlConnectorJava = "mysql" % "mysql-connector-java" % "6.0.5"
  val flyway = "org.flywaydb" % "flyway-core" % "4.0"
  val jodaMapper = "com.github.tototoshi" %% "slick-joda-mapper" % "2.1.0"
  val slickStack = Seq(slick, h2, mysqlConnectorJava, slickHikari, jodaMapper, flyway)

  val scalatest = "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  val unitTestingStack = Seq(scalatest)

  val sigarLoader = "io.kamon" % "sigar-loader" % "1.6.6-rev002"
  val akkaKryo = "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.0"
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaV
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaV
  val akkaRemote = "com.typesafe.akka" %% "akka-remote" % akkaV
  val akkaPersistence = "com.typesafe.akka" %% "akka-persistence" % akkaV
  val levelDb = "org.iq80.leveldb" % "leveldb" % "0.7"
  val levelDbJni = "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
  val akkaPersistenceJdbc = "com.github.dnvriend" %% "akka-persistence-jdbc" % "2.6.12"
  val akkaPersistenceStack = Seq(akkaPersistence, levelDb, levelDbJni, akkaPersistenceJdbc)
  val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % akkaV
  val akkaClusterSharding = "com.typesafe.akka" %% "akka-cluster-sharding" % akkaV
  val akkaClusterMetrics = "com.typesafe.akka" %% "akka-cluster-metrics" % akkaV
  val akkaClusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % akkaV
  val akkaMultiNodeTest = "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaV % Test
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpV
  val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core" % akkaHttpV
  val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV

  val akkaHttpSession = "com.softwaremill.akka-http-session" %% "core" % "0.3.0"

  val akkaHttpStack = Seq(akkaHttpCore, akkaHttpTestkit, akkaHttpSession)
  val akkaClusterStack = Seq(akkaCluster, akkaClusterSharding,
    akkaClusterMetrics, akkaClusterTools, akkaMultiNodeTest, sigarLoader)

  val akkaStack = {
    Seq(akkaActor, akkaRemote, akkaKryo, akkaSlf4j) ++
      akkaHttpStack ++ akkaClusterStack ++ akkaPersistenceStack
  }
}

object Dependencies {

  import Libary._

  val designer = loggingStack ++ circe ++ slickStack ++ akkaStack ++ Seq(
    cats,
    typesafeConfig,
    jodaTime,
    jodaConvert,
    javaxMailSun,
    commonsIo
  ) ++ unitTestingStack

  val fileServer = unitTestingStack
}
