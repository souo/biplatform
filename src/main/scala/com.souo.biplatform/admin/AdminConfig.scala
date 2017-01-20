package com.souo.biplatform.admin

import com.typesafe.config.Config

/**
 * Created by souo on 2016/12/13
 */
trait AdminConfig {
  def rootConfig: Config
  val mysqlDataSourceKey = "admin.dataSource.mysql"
}
