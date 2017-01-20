package com.souo.biplatform.queryrouter.mysql

import slick.driver.MySQLDriver.api._

/**
 * Created by souo on 2017/1/5
 */
class MysqlStorage(config: MysqlStorageConfig) {
  val client = Database.forConfig(config.mysqlStorageConfigKey, config.rootConfig)
}
