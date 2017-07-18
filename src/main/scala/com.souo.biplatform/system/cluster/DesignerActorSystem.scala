package com.souo.biplatform.system.cluster

import java.net.URI

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._

/**
 * @author souo
 */
trait DesignerActorSystem {
  val config: Config = ConfigFactory.load("cluster")

  val node = config.getString("designer.node")
  val sysName = "designer"

  val httpEnable: Boolean = config.as[Option[Boolean]]("designer.http.enable").getOrElse(false)

  implicit val system: ActorSystem = ActorSystem(sysName, ConfigFactory.parseString(
    s"""
      |akka.cluster.roles=[${Roles.designer}]
    """.stripMargin
  ).withFallback(config))

}
