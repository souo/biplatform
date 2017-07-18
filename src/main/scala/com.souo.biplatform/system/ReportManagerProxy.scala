package com.souo.biplatform.system

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

/**
 * @author souo
 */
class ReportManagerProxy(login: String, queryRouteNode: ActorRef) extends Actor with ActorLogging {
  def create(): ActorRef = {
    context.actorOf(
      ReportManager.props(queryRouteNode),
      ReportManager.name(login)
    )
  }
  val reportManager = create()
  context.watch(reportManager)

  override def receive: Receive = active(reportManager)

  def sleep: Receive = {
    case command: ReportManager.Command ⇒
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

    case command: ReportManager.Command ⇒
      actor forward command
  }
}

object ReportManagerProxy {
  def props(login: String, queryRouteNode: ActorRef) = Props(classOf[ReportManagerProxy], login, queryRouteNode)
  val name = "rm-proxy"
}
