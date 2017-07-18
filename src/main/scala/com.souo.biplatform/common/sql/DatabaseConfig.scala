package com.souo.biplatform.common.sql

import com.typesafe.config.Config

import net.ceedubs.ficus.Ficus._

/**
 * @author souo
 */
trait DatabaseConfig {

  import DatabaseConfig._

  def rootConfig: Config

  lazy val dbH2Url = rootConfig.getOrElse[String](s"designer.db.h2.properties.url", "jdbc:h2:file:./data/h2/biplatform")

  lazy val dbMysqlServerNameOpt = rootConfig.as[Option[String]](mysqlServerNameKey).filter(_.nonEmpty)
  lazy val dbMysqlPort = rootConfig.getOrElse[String](mysqlPortKey, "3306")
  lazy val dbMysqlDbName = rootConfig.getOrElse[String](mysqlDbNameKey, "")
  lazy val dbMysqlUsername = rootConfig.getOrElse[String](mysqlUsernameKey, "")
  lazy val dbMysqlPassword = rootConfig.getOrElse[String](mysqlPasswordKey, "")
}

object DatabaseConfig {
  val mysqlServerNameKey = "designer.db.mysql.properties.serverName"
  val mysqlPortKey = "designer.db.mysql.properties.portNumber"
  val mysqlDbNameKey = "designer.db.mysql.properties.databaseName"
  val mysqlUsernameKey = "designer.db.mysql.properties.user"
  val mysqlPasswordKey = "designer.db.mysql.properties.password"
}
