package com.souo.biplatform.cube

import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.Directives._
import com.souo.biplatform.common.api.Response._
import com.souo.biplatform.model.{CubeMeta, CubeSchema}
import com.souo.biplatform.system.CubeNode
import com.souo.biplatform.user.application.UserService
import akka.pattern.ask
import com.souo.biplatform.common.api.{DefaultTimeOut, RoutesSupport}
import com.souo.biplatform.system.CubeNode.NoSuchCube
import com.souo.biplatform.user.api.SessionSupport
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.auto._

/**
  * Created by souo on 2017/1/24
  */
trait CubeRoutes extends RoutesSupport with StrictLogging with SessionSupport with DefaultTimeOut {

  val userService: UserService
  val cubeNode: ActorRef

  val cubeRoutes = path("cubes") {
    get {
      userFromSession { user ⇒
        onSuccess(userService.isAdminUser(user)) {
          case true ⇒
            val cmd = CubeNode.ListAllCube
            onSuccess((cubeNode ? cmd).mapTo[Seq[CubeMeta]]) { metaList ⇒
              complete(ok[Seq[CubeMeta]](metaList))
            }
          case false ⇒
            logger.warn(s"login user ${user.login} is not allow access this resource")
            reject(AuthorizationFailedRejection)
        }
      }
    } ~ post {
      userFromSession { user ⇒
        onSuccess(userService.isAdminUser(user)) {
          case true ⇒
            entity(as[RequestParams.Add]) { add =>
              val cmd = CubeNode.Add(add.name, user.login, add.schema)
              onSuccess((cubeNode ? cmd).mapTo[CubeMeta]) { meta ⇒
                complete(ok[CubeMeta](meta))
              }
            }
          case false ⇒
            logger.warn(s"login user ${user.login} is not allow access this resource")
            reject(AuthorizationFailedRejection)
        }
      }
    } ~ (get & path(Segment).map(UUID.fromString)) { id =>
      userFromSession { user ⇒
        onSuccess(userService.isAdminUser(user)) {
          case true ⇒
            val cmd = CubeNode.Get(id)
            onSuccess(cubeNode ? cmd) {
              case schema: CubeSchema ⇒
                complete(ok(schema))
              case NoSuchCube ⇒
                complete(notFound("不存在或者已删除的cube"))
            }
          case false ⇒
            logger.warn(s"login user ${user.login} is not allow access this resource")
            reject(AuthorizationFailedRejection)
        }
      }
    } ~ (put & path(Segment).map(UUID.fromString)) { id ⇒
      userFromSession { user ⇒
        entity(as[RequestParams.Update]) { update ⇒
          val cmd = CubeNode.UpdateCube(id, update.name, user.login, update.schema)
          onSuccess(cubeNode ? cmd) {
            case NoSuchCube ⇒
              complete(notFound("不存在或者已删除的cube"))
            case meta: CubeMeta ⇒
              complete(ok(meta))
          }
        }
      }
    } ~ (delete & path(Segment).map(UUID.fromString)) { cubeId ⇒
      userFromSession { user ⇒
        val cmd = CubeNode.RemoveCube(cubeId, user.login)
        onSuccess(cubeNode ? cmd) {
          case NoSuchCube ⇒
            complete(notFound("不存在或者已删除的cube"))
          case _ ⇒
            complete(ok())
        }
      }
    }
  }
}
