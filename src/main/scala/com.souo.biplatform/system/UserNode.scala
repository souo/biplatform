package com.souo.biplatform.system

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

/**
 * @author souo
 */
object UserNode {
  def name(login: String): String = login

  def props(queryRouteNode: ActorRef) = {
    Props(classOf[UserNode], queryRouteNode)
  }
  trait Command {
    def login: String
  }
}

class UserNode(queryRouteNode: ActorRef) extends Actor with ActorLogging {

  val login: String = self.path.name

  val rmProxy: ActorRef = context.actorOf(ReportManagerProxy.props(login, queryRouteNode), ReportManagerProxy.name)
  val dmProxy: ActorRef = context.actorOf(DashBoardManagerProxy.props(login), DashBoardManagerProxy.name)

  override def receive: Receive = {
    case command: ReportManager.Command ⇒
      rmProxy forward command
    case command: DashBoardManager.Command ⇒
      dmProxy forward command
  }
}