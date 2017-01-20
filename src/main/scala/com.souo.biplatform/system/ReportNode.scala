package com.souo.biplatform.system

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props, Stash}
import akka.dispatch.ControlMessage
import akka.persistence._
import com.souo.biplatform.model.{QueryModel, ReportQuery}
import com.souo.biplatform.queryrouter.mysql.MysqlStorageLookup
import com.souo.biplatform.system.ReportNode._
/**
 * 我们为每一张报表 维护一个独立的actor
 * 通过 akka Persistent 来持久化 报表的状态
 * 对长时间未使用的报表 可以自动下线，以释放系统资源
 */
class ReportNode extends SleepAbleNode with MysqlStorageLookup with Stash {

  implicit val system: ActorSystem = context.system

  var reportQuery: Option[ReportQuery] = None

  override val sleepAfter: Int = durationSettings("designer.report.sleep-after")

  override def receiveRecover: Receive = {
    case SnapshotOffer(metadata, snapshot: ReportQuery) ⇒
      log.info("Recover from snapshot")
      lastSnapshot = Some(metadata)
      reportQuery = Some(snapshot)
  }

  def save(): Unit = {
    log.info("Saving snapshot")
    saveSnapshot(reportQuery.head)
  }

  def delete(): Unit = {
    log.info("delete snapshot ")
    deleteSnapshots(SnapshotSelectionCriteria.Latest)
  }

  override def receiveCommand: Receive = ({
    case Execute(_, _, queryModel) ⇒
      log.info("start execute")
      storage forward queryModel
      sleep()

    case Save(_, _, query) ⇒
      reportQuery = Some(query)
      save()
      sleep()
      context.become(waitToSaveReply(sender()))

    case Show(_, _) ⇒
      sender() ! reportQuery
      sleep()

    case Clear(_, _) ⇒
      delete()
      sleep()
      context.become(waitToDeleteReply(sender()))
  }: Receive) orElse super.receiveCommand

  def waitToSaveReply(replyTo: ActorRef): Receive = {
    case SaveSnapshotSuccess(metadata) ⇒
      log.info("Snapshot save successfully")
      lastSnapshot = Some(metadata)
      deleteOldSnapshots(false)
      context.unbecome()
      unstashAll()
      replyTo ! SaveSucceed

    case SaveSnapshotFailure(metadata, cause) ⇒
      val msg = s"Snapshot not saved: ${cause.getMessage}"
      log.error(cause, msg)
      context.unbecome()
      unstashAll()
      replyTo ! SaveFailure(msg)
    case msg ⇒
      stash()
  }

  def waitToDeleteReply(replyTo: ActorRef): Receive = {
    case DeleteSnapshotsSuccess(criteria) ⇒
      log.info("Snapshot delete  successfully")
      reportQuery = None
      lastSnapshot = None
      context.unbecome()
      unstashAll()
      replyTo ! DeleteSucceed

    case DeleteSnapshotFailure(metadata, t) ⇒
      log.info("Snapshot delete failure")
      context.unbecome()
      unstashAll()
      replyTo ! DeleteFailure(t.getMessage)
    case msg ⇒
      stash()
  }

}

object ReportNode {

  def props = Props(classOf[ReportNode])

  def name(reportId: UUID) = reportId.toString

  trait Command extends CubeNode.Command {
    val reportId: UUID
  }

  case class ReportControlMessage(reportId: UUID, message: ControlMessage)

  case class Execute(login: String, reportId: UUID, queryModel: QueryModel) extends Command

  case class Save(login: String, reportId: UUID, query: ReportQuery) extends Command

  case class Show(login: String, reportId: UUID) extends Command

  case class Clear(login: String, reportId: UUID) extends Command

  case object SaveSucceed

  case class SaveFailure(msg: String)

  case object DeleteSucceed
  case class DeleteFailure(msg: String)

}
