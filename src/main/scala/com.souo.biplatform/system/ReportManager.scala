package com.souo.biplatform.system

import java.util.UUID

import akka.actor.{ActorRef, Props, Terminated}
import akka.persistence.{RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import ReportManager._
import com.souo.biplatform.model.{Page, ReportMeta, ReportQuery, Result}
import cats.syntax.either._

/**
 * @author souo
 */
class ReportManager(queryRouteNode: ActorRef) extends Node {

  var db: SnapShot = emptySnapShot

  var activeNodes: Map[UUID, ActorRef] = Map.empty[UUID, ActorRef]

  var snapshotEvery: Int = 100
  var receiveCmdCount = 0

  def save(): Unit = {
    receiveCmdCount += 1
    if (receiveCmdCount > snapshotEvery) {
      log.info("Saving snapshot")
      saveSnapshot(db)
    }
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

  private def updateState(evt: Event) = evt match {
    case ReportCreated(meta) ⇒
      db :+= meta
    case ReportUpdated(meta) ⇒
      db = db.updated(db.indexWhere(_.id == meta.id), meta)
    case ReportDropped(id) ⇒
      db = db.filterNot(_.id == id)
    case ReportEdited(id) ⇒
      val index = db.indexWhere(_.id == id)
      db = db.updated(index, db(index).edit)
    case ReportPublished(id) ⇒
      val index = db.indexWhere(_.id == id)
      db = db.updated(index, db(index).publish)
    case ReportCleaned(id) ⇒ //清除发布状态
      val index = db.indexWhere(_.id == id)
      db = db.updated(index, db(index).clean)
  }

  private def updateAndReply(evt: Event)(replyTo: ActorRef) = {
    val res = Either.catchNonFatal{
      updateState(evt)
    }
    replyTo ! res
  }

  private def checkReportName(name: String): Result[Unit] = {
    Either.cond(
      !db.exists(_.name == name),
      (),
      new RuntimeException("报表名重复"))
  }

  private def checkReportId(id: UUID): Result[ReportMeta] = {
    Either.fromOption(
      db.find(_.id == id),
      new RuntimeException(s"No Such report reportId:$id"))
  }

  override def receiveCommand: Receive = {
    case CreateReport(login, reportName) ⇒
      checkReportName(reportName) match {
        case l @ Left(_) ⇒
          sender() ! l
        case Right(_) ⇒
          val replyTo = sender()
          val meta = ReportMeta.create(reportName, login)
          persistAsync(ReportCreated(meta)) { evt ⇒
            updateAndReply(evt)(replyTo)
            save()
          }
      }

    case UpdateReport(login, reportId, reportName) ⇒
      checkReportId(reportId) match {
        case Right(meta) ⇒
          if (meta.name.equals(reportName)) {
            //do nothing just tell the send ok
            sender() ! Right(())
          }
          else {
            checkReportName(reportName) match {
              case Right(_) ⇒
                val newMeta = meta.reName(reportName)
                val replyTo = sender()
                persistAsync(ReportUpdated(newMeta)) { evt ⇒
                  updateAndReply(evt)(replyTo)
                  save()
                }
              case l @ Left(_) ⇒
                sender() ! l
            }
          }
        case l @ Left(_) ⇒
          sender() ! l
      }

    case DropReport(_, id) ⇒
      db.find(_.id == id) match {
        case Some(meta) ⇒
          if (meta.edited) {
            reportNode(meta.id).foreach{ actor ⇒
              actor.tell(Delete, sender())
            }
          }
          else {
            sender() ! Right(())
          }
          self ! ReportDropped(id)
        case None ⇒
          sender() ! Left(new RuntimeException(s"No Such report reportId:$id "))
      }

    case ListAllReport(_, published, page, query) ⇒
      val finalQuery = query.filter(_.nonEmpty)
      val res = (published, finalQuery) match {
        case (true, Some(q)) ⇒
          db.filter{ meta ⇒
            meta.published && meta.name.contains(q)
          }
        case (false, Some(q)) ⇒
          db.filter{ meta ⇒
            meta.name.contains(q)
          }
        case (true, None) ⇒
          db.filter(_.published)
        case other ⇒
          db
      }
      val pageRes = page match {
        case Some(Page(no, size)) ⇒
          val from = (no - 1) * size
          val to = no * size
          res.slice(from, to)
        case None ⇒ res
      }

      sender() ! PageResult(res.length, pageRes)

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

    case evt @ ReportPublished(id) ⇒
      log.info(s"the report $id was published successful")
      persistAsync(evt){
        updateState
      }

    case evt @ ReportEdited(id) ⇒
      log.info(s"report $id was edit successful")
      persistAsync(evt){
        updateState
      }

    case evt @ ReportDropped(id) ⇒
      log.info(s"drop the report $id ")
      persistAsync(evt){
        updateState
      }

    case evt @ ReportCleaned(id) ⇒
      log.info(s"clean the report $id ")
      persistAsync(evt){
        updateState
      }

    case SaveSnapshotSuccess(metadata) ⇒
      log.info("Snapshot save successfully")
      deleteMessages(metadata.sequenceNr - 1)
      lastSnapshot = Some(metadata)
      deleteOldSnapshots()
      receiveCmdCount = 0

    case SaveSnapshotFailure(_, cause) ⇒
      log.error(cause, s"Snapshot not saved: ${cause.getMessage}")

    case command: ReportNode.Command ⇒
      reportNode(command.reportId) match {
        case Right(node) ⇒
          node forward command
        case l @ Left(_) ⇒
          sender() ! l
      }
  }

  private def reportNode(id: UUID): Result[ActorRef] = {
    activeNodes.get(id).fold{
      checkReportId(id).map(_ ⇒ create(id))
    }{ Right.apply }
  }

  private def create(id: UUID): ActorRef = {
    context.actorOf(
      ReportNode.props(queryRouteNode, self),
      ReportNode.name(id))
  }

  def allSoulsReaped(): Unit = {
    log.warning(s"all report node manager by ${self.path} have terminated")
    stop()
  }

}

object ReportManager {

  def props(queryRouteNode: ActorRef) = Props(classOf[ReportManager], queryRouteNode)

  def name(login: String) = s"$login-rm"

  //cmd
  trait Command extends UserNode.Command

  case class CreateReport(login: String, reportName: String) extends Command

  case class UpdateReport(login: String, reportId: UUID, reportName: String) extends Command

  case class DropReport(login: String, reportId: UUID) extends Command

  case class ListAllReport(
    login:     String,
    published: Boolean        = false,
    page:      Option[Page]   = None,
    query:     Option[String] = None) extends Command

  //event
  sealed trait Event

  case class ReportCreated(meta: ReportMeta) extends Event

  case class ReportUpdated(reportMeta: ReportMeta) extends Event

  case class ReportDropped(reportId: UUID) extends Event

  case class ReportPublished(reportId: UUID) extends Event
  case class ReportEdited(reportId: UUID) extends Event

  case class ReportCleaned(reportId: UUID) extends Event

  case class WatchMe(id: UUID, ref: ActorRef)

  case class PageResult(totalCount: Int, reports: Array[ReportMeta])

  type SnapShot = Array[ReportMeta]

  val emptySnapShot: SnapShot = Array.empty[ReportMeta]
}
