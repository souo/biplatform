package com.souo.biplatform.system

import java.util.UUID

import akka.actor._
import com.souo.biplatform.system.ReportNode.ReportControlMessage
import com.souo.biplatform.system.UserNode.WatchMe

/**
 * Created by souo on 2016/12/23
 */
trait ReportLookup {
  implicit def context: ActorContext

  def forwardToReportNode: Actor.Receive = {
    case cmd: ReportNode.Command ⇒
      context.child(ReportNode.name(cmd.reportId)).fold(createAndForward(Left(cmd), cmd.reportId)) {
        forwardCommand(Left(cmd))
      }

    case msg: ReportControlMessage ⇒
      context.child(ReportNode.name(msg.reportId)).fold(createAndForward(Right(msg), msg.reportId)) {
        forwardCommand(Right(msg))
      }
  }

  def forwardCommand(cmd: Either[ReportNode.Command, ReportNode.ReportControlMessage])(reportNode: ActorRef) = {
    cmd match {
      case Left(command) ⇒
        reportNode forward command
      case Right(msg) ⇒
        reportNode forward msg.message
    }
  }

  def createAndForward(cmd: Either[ReportNode.Command, ReportNode.ReportControlMessage], reportId: UUID) = {
    forwardCommand(cmd)(createReport(reportId))
  }

  def createReport(reportId: UUID) = {
    val report = context.actorOf(
      ReportNode.props,
      ReportNode.name(reportId)
    )
    context.self ! WatchMe(reportId, report)
    report
  }

}

trait LocalReportLookUp extends ReportLookup
