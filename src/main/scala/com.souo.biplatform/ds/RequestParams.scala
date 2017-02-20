package com.souo.biplatform.ds

/**
  * Created by souo on 2017/1/24
  */
object RequestParams {

  case class Csv(name: String,
                 path: String,
                 sep: String = ",")

  case class Mysql(name: String,
                   host: String,
                   port: Int,
                   db: String,
                   user: String,
                   pwd: String)

}
