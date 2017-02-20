package com.souo.biplatform

import com.typesafe.config.ConfigFactory

import scala.util.Try

/**
 * Created by souo on 2017/1/24
 */
object SystemEnv {

  val config = ConfigFactory.load("env")
  //用于test
  //打开该选项 不需要从cookie中读取用户名，默认 管理员用户
  val isTest = Try(config.getString("system.env.test").equalsIgnoreCase("on")).getOrElse(false)

}
