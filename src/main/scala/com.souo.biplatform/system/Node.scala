package com.souo.biplatform.system

import akka.actor.ActorLogging
import akka.dispatch.ControlMessage
import akka.persistence.{PersistentActor, SnapshotMetadata, SnapshotSelectionCriteria}

import scala.util.Try
import cats.syntax.either._

/**
 * @author souo
 */
abstract class Node extends PersistentActor with ActorLogging {
  def persistenceId: String = self.path.name

  var lastSnapshot: Option[SnapshotMetadata] = None

  protected val created = System.currentTimeMillis()

  protected def durationSettings(path: String): Int = {
    Either.catchNonFatal(
      context.system.settings.config.getDuration(path).toMillis.toInt
    ).getOrElse(0)
  }

  /**
   * Deletes old snapshots after a new one is saved.
   */
  def deleteOldSnapshots(stopping: Boolean = false): Unit = {
    lastSnapshot.foreach { meta ⇒
      val criteria = if (stopping) {
        SnapshotSelectionCriteria()
      }
      else {
        SnapshotSelectionCriteria(meta.sequenceNr, meta.timestamp - 1)
      }
      log.info("delete old snapshot")
      deleteSnapshots(criteria)
    }
  }

  def stop(): Unit = {
    log.info(s"${self.path} try to stop")
    Try(context.stop(self))
  }

  override def receiveCommand: Receive = {
    case Delete ⇒
      deleteOldSnapshots(stopping = true)
      stop()
    case Sleep ⇒
      stop()
  }
}

/**
 * Message that shuts a Node actor down and delete its value
 */
case object Delete extends ControlMessage

/**
 * Message that tells an actor to shut down, but not delete its value
 */
case object Sleep extends ControlMessage

