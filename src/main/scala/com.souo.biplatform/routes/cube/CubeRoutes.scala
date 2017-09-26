package com.souo.biplatform.routes.cube

import java.util.UUID
import javax.ws.rs.Path

import akka.actor.ActorRef
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Route}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.common.api.{DefaultTimeOut, RoutesSupport, SessionSupport}
import com.souo.biplatform.model.{CubeNameAndSchema, Page, Result}
import com.souo.biplatform.system.CubeNode
import com.souo.biplatform.common.api.Response._
import com.souo.biplatform.routes.user.UserService
import io.circe.generic.auto._
import io.swagger.annotations._
import RequestParams._

/**
 * @author souo
 */
@Api(tags     = Array("cubes"), produces = "application/json")
@Path("/api/cubes")
trait CubeRoutes extends RoutesSupport with StrictLogging with SessionSupport with DefaultTimeOut {

  val userService: UserService
  val cubeNode: ActorRef

  val cubeRoutes: Route = pathPrefix("cubes") {
    listAllCubes ~
      createNewCube ~
      getCubeSchema ~
      updateCube ~
      dropCube
  }

  @Path("")
  @ApiOperation(
    value      = "获取cube列表",
    httpMethod = "GET")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "query",
        paramType = "query",
        dataType  = "string",
        required  = false),
      new ApiImplicitParam(
        name      = "pageNo",
        paramType = "query",
        dataType  = "int",
        required  = false),
      new ApiImplicitParam(
        name      = "pageSize",
        paramType = "query",
        dataType  = "int",
        required  = false)))
  @ApiResponses(
    Array(
      new ApiResponse(
        code     = 200,
        message  = "成功",
        response = classOf[CubeNode.PageResult])))
  def listAllCubes: Route = get {
    pathEnd {
      parameter("pageNo".as[Int] ?, "pageSize".as[Int]?) { (pageNo, pageSize) ⇒
        val page = for {
          n ← pageNo
          s ← pageSize
        } yield Page(n, s)
        parameter("query"?){ query ⇒
          userFromSession { user ⇒
            if (user.isAdmin) {
              val cmd = CubeNode.ListAllCube(page, query)
              onSuccess((cubeNode ? cmd).mapTo[CubeNode.PageResult]) { res ⇒
                complete(ok(res))
              }
            }
            else {
              logger.warn(s"login user ${user.login} is not allow access this resource")
              reject(AuthorizationFailedRejection)
            }
          }
        }
      }
    }
  }

  @Path("")
  @ApiOperation(
    value      = "创建一个新的CUBE",
    httpMethod = "POST")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        paramType = "body",
        dataType  = "com.souo.biplatform.routes.cube.RequestParams$CubeNameAndSchema",
        required  = true)))
  @ApiResponses(
    Array(
      new ApiResponse(
        code    = 200,
        message = "成功")))
  def createNewCube: Route = post {
    pathEnd {
      userFromSession { user ⇒
        if (user.isAdmin) {
          entity(as[RequestParams.CubeNameAndSchema]) { nameAndSchema ⇒
            val cmd = CubeNode.Add(nameAndSchema.cubeName, user.login, nameAndSchema.schema)
            onSuccess((cubeNode ? cmd).mapTo[Result[Unit]]) {
              case Right(_) ⇒
                complete(ok())
              case Left(t) ⇒
                complete(error(t))
            }
          }
        }
        else {
          logger.warn(s"login user ${user.login} is not allow access this resource")
          reject(AuthorizationFailedRejection)
        }
      }
    }
  }

  @Path("/{cubeId}")
  @ApiOperation(
    value      = "获取一个CUBE",
    httpMethod = "GET")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "cubeId",
        paramType = "path",
        dataType  = "java.util.UUID",
        required  = true)))
  @ApiResponses(
    Array(
      new ApiResponse(
        code     = 200,
        message  = "成功",
        response = classOf[CubeNameAndSchema])))
  def getCubeSchema: Route = get {
    path(Segment) { id ⇒
      userFromSession { user ⇒
        if (user.isAdmin) {
          val cmd = CubeNode.Get(UUID.fromString(id))
          onSuccess((cubeNode ? cmd).mapTo[Result[CubeNameAndSchema]]) {
            case Right(schema) ⇒
              complete(ok(schema))
            case Left(t) ⇒
              complete(error(t))
          }
        }
        else {
          logger.warn(s"login user ${user.login} is not allow access this resource")
          reject(AuthorizationFailedRejection)
        }
      }
    }
  }

  @Path("/{cubeId}")
  @ApiOperation(
    value      = "更新一个CUBE",
    httpMethod = "PUT")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "cubeId",
        paramType = "path",
        dataType  = "java.util.UUID",
        required  = true),
      new ApiImplicitParam(
        paramType = "body",
        dataType  = "com.souo.biplatform.routes.cube.RequestParams$CubeNameAndSchema",
        required  = true)))
  @ApiResponses(
    Array(
      new ApiResponse(
        code    = 200,
        message = "成功")))
  def updateCube: Route = put {
    path(Segment) { id ⇒
      userFromSession { user ⇒
        entity(as[RequestParams.CubeNameAndSchema]) { update ⇒
          val cmd = CubeNode.UpdateCube(UUID.fromString(id), update.cubeName, user.login, update.schema)
          onSuccess((cubeNode ? cmd).mapTo[Result[Unit]]) {
            case Right(_) ⇒
              complete(ok())
            case Left(t) ⇒
              complete(error(t))
          }
        }
      }
    }
  }

  @Path("/{cubeId}")
  @ApiOperation(
    value      = "删除一个CUBE",
    httpMethod = "DELETE")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "cubeId",
        paramType = "path",
        dataType  = "java.util.UUID",
        required  = true)))
  @ApiResponses(
    Array(
      new ApiResponse(
        code    = 200,
        message = "成功")))
  def dropCube: Route = delete {
    path(Segment) { cubeId ⇒
      userFromSession { user ⇒
        val cmd = CubeNode.RemoveCube(UUID.fromString(cubeId), user.login)
        onSuccess((cubeNode ? cmd).mapTo[Result[Unit]]) {
          case Right(_) ⇒
            complete(ok())
          case Left(t) ⇒
            complete(error(t))
        }
      }
    }
  }
}
