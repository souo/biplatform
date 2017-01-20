package com.souo.biplatform.queryrouter.mysql

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import com.souo.biplatform.model.QueryModel

/**
 * Created by souo on 2017/1/6
 */
class MysqlStorageNode(engine: MysqlEngine) extends Actor with ActorLogging {

  import context.dispatcher

  override def receive: Receive = {
    case query: QueryModel ⇒
      sender() ! engine.execute(query)
  }
}

object MysqlStorageNode {
  def props(engine: MysqlEngine) = Props(classOf[MysqlStorageNode], engine)

  val name = "MysqlStorage"
}
