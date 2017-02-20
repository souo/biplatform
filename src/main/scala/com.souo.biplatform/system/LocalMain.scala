package com.souo.biplatform.system

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.souo.biplatform.HttpService

/**
 * Created by souo on 2017/1/3
 */
object LocalMain extends App with HttpService {

  implicit val system = ActorSystem("designer", ConfigFactory.load("local"))

  val users = system.actorOf(LocalUsers.props, LocalUsers.name)

  val cubeNode = system.actorOf(CubeNode.props, CubeNode.name)

  val dsNode = system.actorOf(DataSourceNode.props, DataSourceNode.name)

  startService(cubeNode, users, dsNode)
}
