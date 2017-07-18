package com.souo.biplatform.system

import java.util.UUID

import akka.actor.{ActorRef, Props}
import akka.persistence.RecoveryCompleted
import com.souo.biplatform.system.DashBoardNode._
import com.souo.biplatform.system.ReportManager.WatchMe
/**
 * @author souo
 */
class DashBoardNode extends SleepAbleNode {
  var widgets: Array[Widget] = Array.empty[Widget]

  override val sleepAfter: Int = durationSettings("designer.dashBoard.sleep-after")

  private val id = UUID.fromString(persistenceId)
  override def preStart(): Unit = {
    super.preStart()
    context.parent ! WatchMe(id, self)
  }

  private def updateState(evt: Event) = evt match {
    case WidgetAdded(widget) ⇒
      widgets :+= widget
    case WidgetRemove(id) ⇒
      widgets = widgets.filterNot(_.id == id)
  }

  override def receiveRecover: Receive = {
    case evt: Event ⇒
      log.debug("receiveRecover evt " + evt)
      updateState(evt)

    case RecoveryCompleted ⇒
      log.info("Recover completed for {} in {}ms", persistenceId, System.currentTimeMillis() - created)
  }

  override def receiveCommand: Receive = {
    case AddWidget(_, _, rId, visualizationType, size) ⇒
      sleep()
      val newWidget = Widget(UUID.randomUUID(), rId, visualizationType, size)
      persist(WidgetAdded(newWidget)){ evt ⇒
        updateAndReply(evt)(sender())
      }

    case RemoveWidget(_, _, id) ⇒
      sleep()
      persist(WidgetRemove(id)){ evt ⇒
        updateAndReply(evt)(sender())
      }

    case ListAll(_, _) ⇒
      sleep()
      sender() ! widgets

    case Sleep ⇒
      stop()

    case Delete ⇒
      deleteMessages(Long.MaxValue)
      stop()
  }

  private def updateAndReply(evt: Event)(replyTo: ActorRef) = {
    updateState(evt)
    replyTo ! widgets
  }
}

object DashBoardNode {
  def props = Props(classOf[DashBoardNode])
  def name(dashBoardId: UUID): String = dashBoardId.toString

  case class Widget(id: UUID, reportId: UUID, visualizationType: String, size: String)

  trait Command extends UserNode.Command {
    val dashBoardId: UUID
  }
  case class AddWidget(
    login:             String,
    dashBoardId:       UUID,
    reportId:          UUID,
    visualizationType: String,
    size:              String
  ) extends Command

  case class RemoveWidget(login: String, dashBoardId: UUID, id: UUID) extends Command
  case class ListAll(login: String, dashBoardId: UUID)

  trait Event
  case class WidgetAdded(widget: Widget) extends Event
  case class WidgetRemove(reportId: UUID) extends Event
}