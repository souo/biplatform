package com.souo.biplatform.routes.ds

/**
 * @author souo
 */
object RequestParams {

  case class JdbcConfig(
    `type`: String,
    name:   String,
    host:   String,
    port:   Int,
    db:     String,
    user:   String,
    pwd:    String)

}
