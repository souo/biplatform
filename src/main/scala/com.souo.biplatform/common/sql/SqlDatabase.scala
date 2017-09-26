package com.souo.biplatform.common.sql

import com.github.tototoshi.slick.{GenericJodaSupport, H2JodaSupport, MySQLJodaSupport}
import com.typesafe.scalalogging.StrictLogging
import org.flywaydb.core.Flyway
import slick.driver.JdbcProfile
import slick.jdbc.JdbcBackend._

/**
 * @author souo
 */
case class SqlDatabase(
  db:               slick.jdbc.JdbcBackend.Database,
  driver:           JdbcProfile,
  jodaSupport:      GenericJodaSupport,
  connectionString: JdbcConnectionString) extends DBComponent {

  def updateSchema() {
    val flyway = new Flyway()
    flyway.setDataSource(connectionString.url, connectionString.username, connectionString.password)
    flyway.migrate()
  }

  def close() {
    db.close()
  }
}

object SqlDatabase extends StrictLogging {

  def create(config: DatabaseConfig): SqlDatabase = {
    if (config.dbMysqlServerNameOpt.nonEmpty) {
      createMysqlDbFromConfig(config)
    }
    else {
      createEmbedded(config)
    }
  }

  private def createEmbedded(config: DatabaseConfig): SqlDatabase = {
    val db = Database.forConfig("designer.db.h2", config.rootConfig)
    SqlDatabase(db, slick.driver.H2Driver, H2JodaSupport,
                JdbcConnectionString(embeddedConnectionStringFromConfig(config)))
  }

  def embeddedConnectionStringFromConfig(config: DatabaseConfig): String = {
    val url = config.dbH2Url
    val fullPath = url.split(":")(3)
    logger.info(s"Using an embedded database, with data files located at: $fullPath")
    url
  }

  def mysqlJdbcConnectString(config: DatabaseConfig) = {
    val host = config.dbMysqlServerNameOpt.head
    val port = config.dbMysqlPort
    val dbName = config.dbMysqlDbName
    val userName = config.dbMysqlUsername
    val password = config.dbMysqlPassword
    JdbcConnectionString(s"jdbc:mysql://$host:$port/$dbName", userName, password)
  }

  def createMysqlDbFromConfig(config: DatabaseConfig): SqlDatabase = {
    val db = Database.forConfig("designer.db.mysql", config.rootConfig)
    SqlDatabase(db, slick.driver.H2Driver, H2JodaSupport, mysqlJdbcConnectString(config))
  }

}

trait DBComponent {
  val driver: JdbcProfile
  val jodaSupport: GenericJodaSupport
  val db: slick.jdbc.JdbcBackend.Database
}

case class JdbcConnectionString(url: String, username: String = "", password: String = "")

