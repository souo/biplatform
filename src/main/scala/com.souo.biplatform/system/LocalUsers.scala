package com.souo.biplatform.system

import akka.actor.{Actor, ActorContext, ActorRef, Props}

/**
 * Created by souo on 2016/12/26
 */
object LocalUsers {
  def props = Props(classOf[LocalUsers])
  val name = "local-users"
}

class LocalUsers extends Actor with UserLookUp {
  override def receive: Receive = forwardToUser
}

trait UserLookUp {
  implicit def context: ActorContext

  def forwardToUser: Actor.Receive = {
    case cmd: UserNode.Command ⇒
      context.child(UserNode.name(cmd.login))
        .fold(createAndForward(cmd, cmd.login))(forwardCommand(cmd))
  }

  def forwardCommand(cmd: UserNode.Command)(user: ActorRef) =
    user forward cmd

  def createAndForward(cmd: UserNode.Command, login: String) = {
    createUserNode(login) forward cmd
  }

  def createUserNode(login: String) = {
    context.actorOf(
      UserNode.props,
      UserNode.name(login)
    )
  }

}
