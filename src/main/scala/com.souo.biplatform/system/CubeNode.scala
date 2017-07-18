package com.souo.biplatform.system

import java.util.UUID

import akka.actor.{ActorRef, Props}
import akka.persistence._
import com.souo.biplatform.model.{Cube, CubeMeta, CubeNameAndSchema, CubeSchema, Cubes, Page, Result}
import org.joda.time.DateTime
import cats.syntax.either._

/**
 * @author souo
 */
class CubeNode extends Node {

  import CubeNode._

  var cubes = Cubes()
  var snapshotEvery: Int = 10
  var receiveCmdCount = 0

  def updateCubes(evt: Event) = evt match {
    case Added(cube) ⇒
      cubes = cubes.add(cube)
    case CubeRemoved(cubeId) ⇒
      cubes = cubes.removeCube(cubeId)
    case CubeUpdated(cubeId, cube) ⇒
      cubes = cubes.updateCube(cubeId, cube)
  }

  def updateAndReply(evt: Event)(replyTo: ActorRef) = {
    val res = Either.catchNonFatal {
      updateCubes(evt)
    }
    replyTo ! res
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
      cubes.checkName(name) match {
        case Right(_) ⇒
          val cubeId = UUID.randomUUID()
          val meta = CubeMeta(cubeId, name, user, None, DateTime.now())
          val cube = Cube(meta, schema)
          val replyTo = sender()
          persistAsync(Added(cube)) { evt ⇒
            updateAndReply(evt)(replyTo)
            save()
          }
        case l @ Left(_) ⇒
          sender() ! l
      }

    case UpdateCube(id, name, user, schema) ⇒
      cubes.get(id) match {
        case Some(cube) ⇒
          val checkName = if (!cube.meta.cubeName.equals(name)) {
            log.debug(s"rename cube, before:${cube.meta.cubeName}, after:${name}")
            log.debug("开始检查名称是否重复")
            cubes.checkName(name)
          }
          else {
            Right[Throwable, Unit](())
          }
          checkName match {
            case Right(_) ⇒
              val newMeta = cube.meta.copy(
                cubeName      = name,
                modifyBy      = Some(user),
                latModifyTime = DateTime.now()
              )
              val newCube = Cube(newMeta, schema)
              val replyTo = sender()
              persistAsync(CubeUpdated(id, newCube)) { evt ⇒
                updateAndReply(evt)(replyTo)
                save()
              }
            case l @ Left(_) ⇒
              sender() ! l
          }

        case None ⇒
          sender() ! Left(new RuntimeException(s"no such cube $id"))
      }

    case RemoveCube(id, _) ⇒
      cubes.get(id) match {
        case Some(_) ⇒
          val replyTo = sender()
          persistAsync(CubeRemoved(id)) { evt ⇒
            updateAndReply(evt)(replyTo)
            save()
          }
        case None ⇒
          sender() ! Left(new RuntimeException(s"no such cube $id"))
      }

    case ListAllCube(page, query) ⇒
      val metaList = cubes.list.map(_.meta)
      val finalQuery = query.filter(_.nonEmpty)
      val res = finalQuery match {
        case Some(q) ⇒
          metaList.filter{ m ⇒
            m.cubeName.contains(q) || m.createBy.contains(q)
          }
        case None ⇒
          metaList
      }

      val pageRes = page match {
        case Some(Page(n, s)) ⇒
          val from = (n - 1) * s
          val to = n * s
          res.slice(from, to)
        case None ⇒
          res
      }

      sender() ! PageResult(res.size, pageRes)

    case Get(id) ⇒
      val res = Either.fromOption(
        cubes.get(id).map{ cube ⇒
          CubeNameAndSchema(cube.meta.cubeName, cube.schema)
        },
        new RuntimeException(s"no such cube $id")
      )
      sender() ! res

    case SaveSnapshotSuccess(metadata) ⇒
      log.info("Snapshot save successfully")
      deleteMessages(metadata.sequenceNr - 1)
      lastSnapshot = Some(metadata)
      deleteOldSnapshots()
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

  case class UpdateCube(
    cubeId: UUID,
    name:   String,
    login:  String,
    schema: CubeSchema
  ) extends Command

  case class RemoveCube(cubeId: UUID, login: String) extends Command

  //evt message
  sealed trait Event extends Serializable

  case class Added(cube: Cube) extends Event

  case class CubeRemoved(cubeId: UUID) extends Event

  case class CubeUpdated(cubeId: UUID, cube: Cube) extends Event

  case class PageResult(totalCount: Int, cubes: Seq[CubeMeta])

  //other message
  case class ListAllCube(page: Option[Page], query: Option[String])

  case class Get(cubeId: UUID)

  case class CubeCreated(meta: CubeMeta)

  case class Snapshot(cubes: Cubes)

}