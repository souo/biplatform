package com.souo.biplatform.queryrouter.mysql

import akka.actor.ActorSystem

/**
 * Created by souo on 2017/1/6
 */
trait MysqlStorageLookup {
  implicit val system: ActorSystem
  def storage = system.actorSelection(s"/user/${MysqlStorageNode.name}")
}
