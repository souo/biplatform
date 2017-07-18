package com.souo.biplatform.system

import akka.actor.{Actor, ActorContext, ActorRef, Props}

/**
 * @author souo
 */
object LocalUsers {
  def props(queryRouteNode: ActorRef) = Props(classOf[LocalUsers], queryRouteNode)
  val name = "local-users"
}

class LocalUsers(queryRouteNode: ActorRef) extends Actor with UserLookUp {
  override def receive: Receive = forwardToUser(queryRouteNode)
}

trait UserLookUp {
  implicit def context: ActorContext

  def forwardToUser(queryRouteNode: ActorRef): Actor.Receive = {
    case cmd: UserNode.Command ⇒
      context.child(UserNode.name(cmd.login))
        .fold(createAndForward(cmd, cmd.login, queryRouteNode))(forwardCommand(cmd))
  }

  def forwardCommand(cmd: UserNode.Command)(user: ActorRef): Unit = {
    user forward cmd
  }

  def createAndForward(cmd: UserNode.Command, login: String, queryRouteNode: ActorRef): Unit = {
    createUserNode(login, queryRouteNode) forward cmd
  }

  def createUserNode(login: String, queryRouteNode: ActorRef): ActorRef = {
    context.actorOf(
      UserNode.props(queryRouteNode),
      UserNode.name(login)
    )
  }

}
