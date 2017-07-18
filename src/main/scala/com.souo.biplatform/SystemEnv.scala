package com.souo.biplatform

import com.typesafe.config.ConfigFactory

import scala.util.Try

/**
 * @author souo
 */
object SystemEnv {

  val config = ConfigFactory.load("env")
  //用于test
  //打开该选项 不需要从cookie中读取用户名，默认 "admin"
  val isTest = Try{
    config.getString("system.env.test").equalsIgnoreCase("on")
  }.getOrElse(false)

}
