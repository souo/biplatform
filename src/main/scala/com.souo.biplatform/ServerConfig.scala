package com.souo.biplatform

import com.typesafe.config.Config
import cats.syntax.either._

/**
 * @author souo
 */
trait ServerConfig {

  def rootConfig: Config

  lazy val serverHost: String = Either.catchNonFatal(
    rootConfig.getString("designer.http.server.host")
  ).getOrElse("0.0.0.0")

  lazy val serverPort: Int = Either.catchNonFatal(
    rootConfig.getInt("designer.http.server.port")
  ).getOrElse(8080)
}
