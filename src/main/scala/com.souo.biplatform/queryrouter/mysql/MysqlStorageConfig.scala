package com.souo.biplatform.queryrouter.mysql

import com.typesafe.config.Config

/**
 * Created by souo on 2017/1/6
 */
trait MysqlStorageConfig {
  def rootConfig: Config
  val mysqlStorageConfigKey = "queryrouter.mysql"
}
