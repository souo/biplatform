package com.souo.biplatform.admin

import akka.actor.ActorRef
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.common.api.RoutesSupport
import com.souo.biplatform.user.api.SessionSupport
import com.souo.biplatform.user.application.UserService
import com.souo.biplatform.common.api.Response._
import io.circe.generic.auto._
import akka.pattern.ask
import com.souo.biplatform.admin.domain.RequestParams._
import com.souo.biplatform.system.CubeNode.{NoSuchCube, _}
import java.util.UUID

import com.souo.biplatform.admin.domain.RequestParams
import com.souo.biplatform.model.{CubeMeta, CubeSchema}
import com.souo.biplatform.system.CubeNode
import com.souo.biplatform.system.CubeNode._

import scala.concurrent.duration._

/**
 * Created by souo on 2016/12/13
 */
trait AdminRoutes extends RoutesSupport with StrictLogging with SessionSupport {

  val dataSourceService: DataSourceService
  val userService: UserService
  val cubeNode: ActorRef

  val adminRoutes = pathPrefix("admin") {
    pathPrefix("tables") {
      (get & pathEnd) {
        userFromSession { user ⇒
          onSuccess(userService.isAdminUser(user)) {
            case true ⇒
              dataSourceService.listAllTables() match {
                case Left(t) ⇒
                  complete(error(t))
                case Right(r) ⇒
                  complete(ok(r))
              }
            case false ⇒
              logger.warn(s"login user ${user.login} is not allow access this resource")
              reject(AuthorizationFailedRejection)
          }
        }
      } ~ (get & path(Segment / "fieldsInfo")) { tableId ⇒
        userFromSession { user ⇒
          onSuccess(userService.isAdminUser(user)) {
            case true ⇒
              dataSourceService.listAllColumns(tableId) match {
                case Left(t) ⇒
                  complete(error(t))
                case Right(r) ⇒
                  complete(ok(r))
              }
            case false ⇒
              logger.warn(s"login user ${user.login} is not allow access this resource")
              reject(AuthorizationFailedRejection)
          }
        }
      }
    } ~ pathPrefix("cubes") {
      pathEnd {
        post {
          entity(as[CreateCubeParams]) { params ⇒
            userFromSession { user ⇒
              val cmd = Add(params.name, user.login, params.schema)
              onSuccess((cubeNode ? cmd) (5 seconds).mapTo[CubeMeta]) { meta ⇒
                complete(ok[CubeMeta](meta))
              }
            }
          }
        } ~ get {
          val msg = ListAllCube
          onSuccess((cubeNode ? msg) (5 seconds).mapTo[Seq[CubeMeta]]) { metaList ⇒
            complete(ok[Seq[CubeMeta]](metaList))
          }
        }
      } ~ (get & path(Segment / "schema")) { cubeId ⇒
        val msg = GetCubeSchema(UUID.fromString(cubeId))
        onSuccess((cubeNode ? msg) (5 seconds)) {
          case schema: CubeSchema ⇒
            complete(ok(schema))
          case NoSuchCube ⇒
            complete(notFound("不存在或者已删除的cube"))
        }
      } ~ (put & path(Segment)) { cubeId ⇒
        userFromSession { user ⇒
          entity(as[RequestParams.UpdateCube]) { update ⇒
            val cmd = CubeNode.UpdateCube(UUID.fromString(cubeId), update.name, user.login, update.schema)
            onSuccess((cubeNode ? cmd) (5 seconds)) {
              case NoSuchCube ⇒
                complete(notFound("不存在或者已删除的cube"))
              case meta: CubeMeta ⇒
                complete(ok(meta))
            }
          }
        }
      } ~ (delete & path(Segment)) { cubeId ⇒
        userFromSession { user ⇒
          val cmd = CubeNode.RemoveCube(UUID.fromString(cubeId), user.login)
          onSuccess((cubeNode ? cmd) (5 seconds)) {
            case NoSuchCube ⇒
              complete(notFound("不存在或者已删除的cube"))
            case _ ⇒
              complete(ok())
          }
        }
      }
    }
  }
}

