package com.souo.biplatform.system

import akka.actor._

/**
 * @author souo
 */
class DashBoardManagerProxy(login: String) extends Actor with ActorLogging {

  def create(): ActorRef = {
    context.actorOf(
      DashBoardManager.props(),
      DashBoardManager.name(login)
    )
  }

  override def receive: Receive = {
    val dm = create()
    context.watch(dm)
    active(dm)
  }

  def sleep: Receive = {
    case command: DashBoardManager.Command ⇒
      val actor = create()
      context.watch(actor)
      actor forward command
      context.become(active(actor))
    case _ ⇒
      log.error("un except message")
  }

  def active(actor: ActorRef): Receive = {
    case Terminated(actorRef) ⇒
      log.info(s"Actor $actorRef terminated.")
      log.info("switching to sleep state")
      context.become(sleep)

    case command: DashBoardManager.Command ⇒
      actor forward command
  }
}

object DashBoardManagerProxy {
  def props(login: String) = Props(classOf[DashBoardManagerProxy], login)
  val name = "dm-proxy"
}
