package com.souo.biplatform.system

import java.util.UUID

import akka.actor.Props
import akka.persistence._
import com.souo.biplatform.model.{Cube, CubeMeta, CubeSchema, Cubes}
import org.joda.time.DateTime
import CubeNode._

/**
  * Created by souo on 2016/12/13
  */
class CubeNode extends Node {

  var cubes = Cubes()
  var snapshotEvery: Int = 10
  var receiveCmdCount = 0

  override def persistenceId: String = CubeNode.name

  def updateCubes(evt: Event) = evt match {
    case Added(cube) ⇒
      cubes = cubes.add(cube)
    case CubeRemoved(cubeId) ⇒
      cubes = cubes.removeCube(cubeId)
    case CubeUpdated(cubeId, cube) ⇒
      cubes = cubes.updateCube(cubeId, cube)
  }

  override def receiveRecover: Receive = {
    case evt: Event ⇒
      updateCubes(evt)
    case SnapshotOffer(metadata, snapshot: Snapshot) ⇒
      log.info("Recover from snapshot")
      lastSnapshot = Some(metadata)
      cubes = snapshot.cubes
    case RecoveryCompleted ⇒
      log.info("Recover completed for {} in {}ms", persistenceId, System.currentTimeMillis() - created)
  }

  def save(): Unit = {
    receiveCmdCount += 1
    if (receiveCmdCount > snapshotEvery) {
      log.info("Saving snapshot")
      saveSnapshot(Snapshot(cubes))
    }
  }

  override def receiveCommand: Receive = {
    case Add(name, user, schema) ⇒
      val cubeId = UUID.randomUUID()
      val meta = CubeMeta(cubeId, name, user, None, DateTime.now())
      val cube = Cube(meta, schema)
      val replyTo = sender()
      persistAsync(Added(cube)) { evt ⇒
        updateCubes(evt)
        save()
        replyTo ! meta
      }
    case UpdateCube(id, name, user, schema) ⇒
      cubes.get(id) match {
        case Some(cube) ⇒
          val newMeta = cube.meta.copy(
            cubeName = name,
            modifyBy = Some(user),
            latModifyTime = DateTime.now()
          )
          val newCube = Cube(newMeta, schema)
          val replyTo = sender()
          persistAsync(CubeUpdated(id, newCube)) { evt ⇒
            updateCubes(evt)
            save()
            replyTo ! newMeta
          }
        case None ⇒
          sender() ! NoSuchCube
      }

    case RemoveCube(id, _) ⇒
      cubes.get(id) match {
        case Some(cube) ⇒
          val replyTo = sender()
          persistAsync(CubeRemoved(id)) { evt ⇒
            updateCubes(evt)
            save()
            replyTo ! None
          }
        case None ⇒
          sender() ! NoSuchCube
      }

    case ListAllCube ⇒
      val metaList = cubes.list.map(_.meta)
      sender() ! metaList

    case GetCubeSchema(cubeId) ⇒
      cubes.list.find(_.meta.cubeId == cubeId) match {
        case Some(cube) ⇒
          sender() ! cube.schema
        case None ⇒
          sender() ! NoSuchCube
      }

    case SaveSnapshotSuccess(metadata) ⇒
      log.info("Snapshot save successfully")
      deleteMessages(metadata.sequenceNr - 1)
      lastSnapshot = Some(metadata)
      deleteOldSnapshots(false)
      receiveCmdCount = 0

    case SaveSnapshotFailure(metadata, cause) ⇒
      log.error(cause, s"Snapshot not saved: ${cause.getMessage}")
  }
}

object CubeNode {

  def props = Props(classOf[CubeNode])

  def name = "GlobalCube"

  //cmd  message
  trait Command extends UserNode.Command

  case class Add(name: String, login: String, schema: CubeSchema) extends Command

  case class UpdateCube(cubeId: UUID,
                        name: String,
                        login: String,
                        schema: CubeSchema) extends Command

  case class RemoveCube(cubeId: UUID, login: String) extends Command

  //evt message
  sealed trait Event extends Serializable

  case class Added(cube: Cube) extends Event

  case class CubeRemoved(cubeId: UUID) extends Event

  case class CubeUpdated(cubeId: UUID, cube: Cube) extends Event

  //other message
  case object ListAllCube

  case class Get(cubeId: UUID)

  case object NoSuchCube

  case class CubeCreated(meta: CubeMeta)

  case class Snapshot(cubes: Cubes)

}
