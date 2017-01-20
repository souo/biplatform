package com.souo.biplatform

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.admin.{AdminConfig, DataSourceService}
import com.souo.biplatform.common.sql.{DatabaseConfig, SqlDatabase}
import com.souo.biplatform.queryrouter.mysql.MysqlStorageConfig
import com.souo.biplatform.user.application.{UserRuleDao, UserService}

/**
 * @author souo
 */
trait DependencyWiring extends StrictLogging {
  def system: ActorSystem

  val config: ServerConfig with DatabaseConfig with AdminConfig with MysqlStorageConfig

  lazy val daoExecutionContext = system.dispatchers.lookup("dao-dispatcher")

  lazy val sqlDatabase = SqlDatabase.create(config)
  lazy val userRuleDao = new UserRuleDao(sqlDatabase)(daoExecutionContext)

  lazy val userService = new UserService(userRuleDao)(daoExecutionContext)

  lazy val dataSourceService: DataSourceService = new DataSourceService(config)
}
