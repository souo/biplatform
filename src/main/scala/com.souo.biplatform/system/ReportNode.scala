package com.souo.biplatform.system

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props, Stash}
import akka.persistence._
import com.souo.biplatform.model.{QueryModel, ReportQuery}
import com.souo.biplatform.system.ReportManager.WatchMe
import cats.syntax.either._

class ReportNode(
  queryRouteNode: ActorRef,
  statusKeeper:   ActorRef)
  extends SleepAbleNode with Stash {

  import ReportNode._

  implicit val system: ActorSystem = context.system

  private val id = UUID.fromString(persistenceId)

  var reportQuery: Option[ReportQuery] = None

  override val sleepAfter: Int = durationSettings("designer.report.sleep-after")

  override def preStart(): Unit = {
    super.preStart()
    statusKeeper ! WatchMe(id, self)
  }

  override def receiveRecover: Receive = {
    case SnapshotOffer(metadata, snapshot: ReportQuery) ⇒
      log.info("Recover from snapshot")
      lastSnapshot = Some(metadata)
      reportQuery = Some(snapshot)
  }

  def save(query: ReportQuery): Unit = {
    log.info("Saving snapshot")
    saveSnapshot(query)
  }

  def delete(): Unit = {
    log.info("delete snapshot ")
    deleteSnapshots(SnapshotSelectionCriteria.Latest)
  }

  override def receiveCommand: Receive = {
    case Execute(_, _) ⇒
      val filteredQuery = reportQuery.filter{ r ⇒
        r.queryModel != null && r.cubeId != null
      }
      filteredQuery match {
        case None ⇒
          sender() ! Left(new RuntimeException("不能执行一张空的报表"))
        case Some(rq) ⇒
          queryRouteNode forward rq
      }

    case GetDimValues(_, _, dimName) ⇒
      reportQuery match {
        case None ⇒
          sender() ! Left(new RuntimeException("请先定义维度和指标"))
        case Some(rq) ⇒
          val cubeId = rq.cubeId
          val tableName = rq.queryModel.tableName
          val cmd = QueryRouteNode.GetDimValues(cubeId, tableName, dimName)
          queryRouteNode forward cmd
      }

    case Publish(_, _) ⇒
      reportQuery match {
        case None ⇒
          sender() ! Left(new RuntimeException("不能发布一张空的报表"))
        case Some(rq) ⇒
          sender() ! Right(())
          statusKeeper ! ReportManager.ReportPublished(id)
      }

    case Save(_, _, query) ⇒
      sleep()
      val check = query.queryModel.check().toEither.leftMap{ r ⇒
        new RuntimeException(r.toList.mkString(";"))
      }

      check match {
        case Right(_) ⇒
          save(query)
          context.become(waitToSaveReply(sender(), query))
        case l @ Left(_) ⇒
          sender() ! l

      }

    case UpdateExtendArea(_, _, props) ⇒
      sleep()
      val query = reportQuery.fold(
        ReportQuery(
          null.asInstanceOf[QueryModel],
          null.asInstanceOf[UUID],
          props)){ r ⇒
          val newProps = r.properties ++ props
          r.copy(properties = newProps)
        }
      save(query)
      context.become(waitToSaveReply(sender(), query))

    case Show(_, _) ⇒
      sender() ! reportQuery
      sleep()

    case Clear(_, _) ⇒
      delete()
      sleep()
      context.become(waitToDeleteReply(sender()))

    case Sleep ⇒
      stop()

    case Delete ⇒
      deleteOldSnapshots(stopping = true)
      stop()
  }

  def waitToSaveReply(replyTo: ActorRef, query: ReportQuery): Receive = {
    case SaveSnapshotSuccess(metadata) ⇒
      replyTo ! Right(())
      statusKeeper ! ReportManager.ReportEdited(id)
      reportQuery = Some(query)
      log.info("Snapshot save successfully")
      lastSnapshot = Some(metadata)
      deleteOldSnapshots()
      context.unbecome()

    case SaveSnapshotFailure(metadata, cause) ⇒
      replyTo ! Left(cause)
      val msg = s"Snapshot not saved: ${cause.getMessage}"
      log.error(cause, msg)
      context.unbecome()
      unstashAll()

    case msg ⇒
      stash()
  }

  def waitToDeleteReply(replyTo: ActorRef): Receive = {
    case DeleteSnapshotsSuccess(criteria) ⇒
      replyTo ! Right(())
      reportQuery = None
      lastSnapshot = None
      //mark the report unpublished
      statusKeeper ! ReportManager.ReportCleaned(id)
      log.info("Snapshot delete  successfully")
      context.unbecome()
      unstashAll()

    case DeleteSnapshotFailure(metadata, t) ⇒
      replyTo ! Left(t)
      log.info("Snapshot delete failure")
      context.unbecome()
      unstashAll()
    case msg ⇒
      stash()
  }

}

object ReportNode {

  def props(queryRouteNode: ActorRef, statusKeeper: ActorRef) = {
    Props(classOf[ReportNode], queryRouteNode, statusKeeper)
  }

  def name(reportId: UUID): String = reportId.toString

  trait Command extends ReportManager.Command {
    val reportId: UUID
  }

  case class Execute(login: String, reportId: UUID) extends Command

  case class Save(login: String, reportId: UUID, query: ReportQuery) extends Command

  case class Publish(login: String, reportId: UUID) extends Command

  case class Show(login: String, reportId: UUID) extends Command

  case class Clear(login: String, reportId: UUID) extends Command

  case class UpdateExtendArea(login: String, reportId: UUID, properties: Map[String, String]) extends Command

  case class GetDimValues(login: String, reportId: UUID, dimName: String) extends Command
}