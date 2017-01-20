package com.souo.biplatform.system

import java.util.UUID

import akka.actor.{ActorRef, Props, Terminated}
import akka.persistence.{RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.souo.biplatform.model.ReportMeta
import com.souo.biplatform.system.UserNode._
import org.joda.time.DateTime
import cats.syntax.either._

import scala.collection.mutable.ArrayBuffer

/**
 * Created by souo on 2016/12/20
 */
object UserNode {

  def props = Props(classOf[UserNode])

  def name(login: String) = login

  //cmd
  trait Command {
    def login: String
  }

  case class CreateReport(login: String, reportName: String) extends Command

  case class UpdateReport(login: String, reportId: UUID, reportName: String) extends Command

  case class DropReport(login: String, reportId: UUID) extends Command

  case class ListAllReport(login: String) extends Command

  //evt
  sealed trait Event

  case class ReportCreated(meta: ReportMeta) extends Event

  case class ReportUpdated(reportMeta: ReportMeta) extends Event

  case class ReportDropped(reportId: UUID) extends Event

  case class NoSuchReportError(reportId: UUID) extends Throwable(s"No Such report reportId:$reportId ")

  case class WatchMe(id: UUID, ref: ActorRef)

  case class AddToCleanUp(id: UUID) extends Event
  case class RemoveFromCleanUp(id: UUID) extends Event

  case class SnapShot(reports: Array[ReportMeta], needCleanUps: Set[UUID])
}

/**
 *  维护报表的元数据
 *  创建和管理子报表 actor
 *  作为路由器 向reportNode 路由消息
 *  当所有打开的 reportNode关闭时 会关闭自身
 *  长时间未使用，会主动陷入休眠，已释放系统资源
 */
class UserNode extends SleepAbleNode with LocalReportLookUp {

  var reports: Array[ReportMeta] = Array.empty[ReportMeta]
  var needCleanUps: Set[UUID] = Set.empty[UUID]
  val activeNodes: ArrayBuffer[ActorRef] = ArrayBuffer.empty[ActorRef]

  override val sleepAfter: Int = durationSettings("designer.user.sleep-after")

  var snapshotEvery: Int = 10
  var receiveCmdCount = 0

  val login = context.self.path.name

  def save(): Unit = {
    receiveCmdCount += 1
    if (receiveCmdCount > snapshotEvery) {
      log.info("Saving snapshot")
      saveSnapshot(SnapShot(reports, needCleanUps))
    }
  }

  override def receiveRecover: Receive = {
    case evt: Event ⇒
      log.debug("receiveRecover evt " + evt)
      updateState(evt)

    case SnapshotOffer(metadata, snapshot: SnapShot) ⇒
      log.info("Recover from snapshot")
      lastSnapshot = Some(metadata)
      reports = snapshot.reports
      needCleanUps = snapshot.needCleanUps

    case RecoveryCompleted ⇒
      log.info("Recover completed for {} in {}ms", persistenceId, System.currentTimeMillis() - created)
  }

  private def updateState(evt: Event) = evt match {
    case ReportCreated(meta) ⇒
      reports :+= meta
    case ReportUpdated(meta) ⇒
      reports = reports.updated(reports.indexWhere(_.id == meta.id), meta)
    case ReportDropped(id) ⇒
      reports = reports.filterNot(_.id == id)
    case AddToCleanUp(id) ⇒
      needCleanUps += id
    case RemoveFromCleanUp(id) ⇒
      needCleanUps -= id
  }

  private def updateAndReply(evt: Event)(replyTo: ActorRef) = {
    val res = Either.catchNonFatal {
      updateState(evt)
      reports
    }
    replyTo ! res
  }

  override def receiveCommand: Receive = forwardToReportNode orElse ({
    case CreateReport(login, reportName) ⇒
      val meta = ReportMeta(
        id            = UUID.randomUUID(),
        name          = reportName,
        createBy      = login,
        createTime    = DateTime.now(),
        modifyBy      = None,
        latModifyTime = None
      )
      val replyTo = sender()
      persistAsync(ReportCreated(meta)) { evt ⇒
        updateAndReply(evt)(replyTo)
        save()
        sleep()
      }

    case UpdateReport(login, reportId, reportName) ⇒
      Either.fromOption(reports.find(_.id == reportId), NoSuchReportError(reportId)) match {
        case l @ Left(error) ⇒
          sender() ! l
        case Right(oldMeta) ⇒
          val newMeta = oldMeta.copy(name = reportName, modifyBy = Some(login), latModifyTime = Some(DateTime.now()))
          val replyTo = sender()
          persistAsync(ReportUpdated(newMeta)) { evt ⇒
            updateAndReply(evt)(replyTo)
            save()
            sleep()
          }
      }

    case DropReport(login, id) ⇒
      val replyTo = sender()
      persistAsync(ReportDropped(id)) { evt ⇒
        //更新自身状态
        updateAndReply(evt)(replyTo)
        //如果需要清除  向report节点 发送删除命令
        if (needCleanUps.contains(id)) {
          self ! ReportNode.ReportControlMessage(id, Delete)
          //从待清除节点 中移除
          persistAsync(RemoveFromCleanUp(id)){ evt ⇒
            updateState(evt)
            save()
            sleep()
          }
        }
      }

    case ListAllReport(_) ⇒
      sender() ! reports

    case WatchMe(id, ref) ⇒
      log.info("Watching actor {}", ref)
      context.watch(ref)
      activeNodes += ref
      //
      if (!needCleanUps.contains(id)) {
        persistAsync(AddToCleanUp(id)){ updateState }
      }

    case Terminated(ref) ⇒
      log.info("Actor {} terminated", ref)
      activeNodes -= ref
      if (activeNodes.isEmpty) {
        allSoulsReaped()
      }

    case SaveSnapshotSuccess(metadata) ⇒
      log.info("Snapshot save successfully")
      deleteMessages(metadata.sequenceNr - 1)
      lastSnapshot = Some(metadata)
      deleteOldSnapshots(false)
      receiveCmdCount = 0

    case SaveSnapshotFailure(metadata, cause) ⇒
      log.error(cause, s"Snapshot not saved: ${cause.getMessage}")

    case Sleep ⇒
      if (activeNodes.isEmpty) {
        stop()
      }
  }: Receive)

  def allSoulsReaped(): Unit = {
    log.warning(s"all report node manager by $login have terminated")
    //    stop()
  }
}
