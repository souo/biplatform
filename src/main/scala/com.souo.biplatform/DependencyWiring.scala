package com.souo.biplatform

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.common.sql.{DatabaseConfig, SqlDatabase}
import com.souo.biplatform.routes.user.{UserRuleDao, UserService}

/**
 * @author souo
 */
trait DependencyWiring extends StrictLogging {
  def system: ActorSystem
  val config: ServerConfig with DatabaseConfig
  lazy val daoExecutionContext = system.dispatchers.lookup("dao-dispatcher")
  lazy val sqlDatabase = SqlDatabase.create(config)
  lazy val userRuleDao = new UserRuleDao(sqlDatabase)(daoExecutionContext)
  lazy val userService = new UserService(userRuleDao)(daoExecutionContext)

}
