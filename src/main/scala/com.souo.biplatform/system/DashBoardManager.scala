package com.souo.biplatform.system

import java.util.UUID

import akka.actor.{ActorRef, Props, Terminated}
import akka.persistence.{RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.souo.biplatform.model.{DashBoard, Page}
import com.souo.biplatform.system.DashBoardManager._
import com.souo.biplatform.common.api.Response._
/**
 * @author souo
 */
class DashBoardManager extends Node {

  var db: SnapShot = emptySnapShot

  var activeNodes: Map[UUID, ActorRef] = Map.empty[UUID, ActorRef]

  var snapshotEvery: Int = 30
  var receiveCmdCount = 0

  def save(): Unit = {
    receiveCmdCount += 1
    if (receiveCmdCount > snapshotEvery) {
      log.info("Saving snapshot")
      saveSnapshot(db)
    }
  }

  private def updateState(evt: Event) = evt match {
    case DashBoardCreated(dashBoard) ⇒
      db :+= dashBoard
    case DashBoardRename(id, name) ⇒
      db = db.updated(db.indexWhere(_.id == id), DashBoard(id, name))
    case DashBoardDropped(id) ⇒
      db = db.filterNot(_.id == id)
  }

  override def receiveRecover: Receive = {
    case evt: Event ⇒
      log.debug("receiveRecover evt " + evt)
      updateState(evt)

    case SnapshotOffer(metadata, snapshot: SnapShot) ⇒
      log.info("Recover from snapshot")
      lastSnapshot = Some(metadata)
      db = snapshot

    case RecoveryCompleted ⇒
      log.info("Recover completed for {} in {}ms", persistenceId, System.currentTimeMillis() - created)
  }

  private def updateAndReply(evt: Event)(replyTo: ActorRef) = {
    updateState(evt)
    replyTo ! db
  }

  override def receiveCommand: Receive = {
    case CreateDashBoard(_, name) ⇒
      val newDashBoard = DashBoard(UUID.randomUUID(), name)
      val replyTo = sender()
      persist(DashBoardCreated(newDashBoard)){ evt ⇒
        updateAndReply(evt)(replyTo)
        save()
      }

    case RenameDashBoard(_, id, name) ⇒
      val replyTo = sender()
      persist(DashBoardRename(id, name)){ evt ⇒
        updateAndReply(evt)(replyTo)
        save()
      }

    case DropDashBoard(_, id) ⇒
      val replyTo = sender()
      persist(DashBoardDropped(id)){ evt ⇒
        updateAndReply(evt)(replyTo)
        dashBoardNode(id).foreach{ _ ! Delete }
        save()
      }

    case ListAllDashBoard(_, page) ⇒
      val res = page match {
        case Some(Page(no, size)) ⇒
          val from = (no - 1) * size
          val to = no * size
          db.slice(from, to)
        case None ⇒ db
      }
      sender() ! res

    case WatchMe(id, ref) ⇒
      log.info("Watching actor {}", ref)
      context.watch(ref)
      activeNodes += id → ref

    case Terminated(ref) ⇒
      log.info("Actor {} terminated", ref)
      val id = UUID.fromString(ref.path.name)
      activeNodes = activeNodes.filterKeys(_ != id)
      if (activeNodes.isEmpty) {
        allSoulsReaped()
      }

    case SaveSnapshotSuccess(metadata) ⇒
      log.info("Snapshot save successfully")
      deleteMessages(metadata.sequenceNr - 1)
      lastSnapshot = Some(metadata)
      deleteOldSnapshots()
      receiveCmdCount = 0

    case SaveSnapshotFailure(_, cause) ⇒
      log.error(cause, s"Snapshot not saved: ${cause.getMessage}")

    case command: DashBoardNode.Command ⇒
      dashBoardNode(command.dashBoardId) match {
        case Some(node) ⇒
          node forward command
        case None ⇒
          val msg = s"no such dashboard ${command.dashBoardId}"
          sender() ! errorMsg(msg)
      }

  }

  private def dashBoardNode(id: UUID): Option[ActorRef] = {
    activeNodes.get(id) orElse {
      db.find(_.id == id).map{ meta ⇒
        create(meta.id)
      }
    }
  }

  private def create(id: UUID): ActorRef = {
    context.actorOf(
      DashBoardNode.props,
      DashBoardNode.name(id))
  }

  def allSoulsReaped(): Unit = {
    log.warning(s"all dashBoard node manager by ${self.path} have terminated")
    stop()
  }

}

object DashBoardManager {

  def props() = Props(classOf[DashBoardManager])

  def name(login: String) = s"$login-dm"

  //command
  sealed trait Command extends UserNode.Command
  case class CreateDashBoard(login: String, dashboardName: String) extends Command
  case class RenameDashBoard(login: String, dashBoardId: UUID, dashboardName: String) extends Command
  case class DropDashBoard(login: String, dashBoardId: UUID) extends Command
  case class ListAllDashBoard(login: String, page: Option[Page]) extends Command

  //event
  sealed trait Event
  case class DashBoardCreated(dashBoard: DashBoard) extends Event
  case class DashBoardRename(dashBoardId: UUID, dashboardName: String) extends Event
  case class DashBoardDropped(dashBoardId: UUID) extends Event

  //
  case class WatchMe(id: UUID, ref: ActorRef)

  type SnapShot = Array[DashBoard]
  val emptySnapShot: SnapShot = Array.empty[DashBoard]
}