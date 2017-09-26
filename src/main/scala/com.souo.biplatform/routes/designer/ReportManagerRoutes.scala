package com.souo.biplatform.routes.designer

import javax.ws.rs.Path

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.common.api.{DefaultTimeOut, RoutesSupport, SessionSupport}
import com.souo.biplatform.common.api.Response._
import com.souo.biplatform.model.{Page, Result}
import com.souo.biplatform.system.ReportManager
import io.circe.generic.auto._
import akka.pattern.ask
import io.swagger.annotations._

/**
 * @author souo
 */
@Api(tags     = Array("reports-manager"), produces = "application/json")
@Path("/api/designer/reports")
trait ReportManagerRoutes extends RoutesSupport with StrictLogging with SessionSupport with DefaultTimeOut {

  val users: ActorRef

  val reportManagerRoutes = pathPrefix("reports") {
    listAllReport ~
      createReport ~
      updateReport ~
      deleteReport
  }

  @Path("")
  @ApiOperation(
    value      = "获取reports列表",
    notes      = "通过发布状态、报表名分页参数筛选指定的reports列表",
    httpMethod = "GET")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name         = "published",
        value        = "发布状态，为true筛选已发布报表，不传或者为false筛选全部报表",
        paramType    = "query",
        dataType     = "boolean",
        required     = false,
        defaultValue = "false"),
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
        response = classOf[ReportManager.PageResult])))
  def listAllReport: Route = (get & pathEnd) {
    userFromSession { user ⇒
      parameter("published" ? false) { published ⇒
        parameter("query" ?) { query ⇒
          parameter("pageNo".as[Int] ?, "pageSize".as[Int] ?) {
            case (pageNo, pageSize) ⇒
              val page = for {
                no ← pageNo
                size ← pageSize
              } yield {
                Page(no, size)
              }
              val cmd = ReportManager.ListAllReport(user.login, published, page, query)
              onSuccess((users ? cmd).mapTo[ReportManager.PageResult]) { res ⇒
                complete(ok(res))
              }
          }
        }
      }
    }
  }

  @Path("")
  @ApiOperation(
    value      = "创建一个新的report",
    httpMethod = "POST")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        paramType = "body",
        dataType  = "com.souo.biplatform.routes.designer.RequestParams$CreateReport",
        required  = true)))
  @ApiResponses(
    Array(
      new ApiResponse(
        code    = 200,
        message = "成功")))
  def createReport: Route = {
    post {
      pathEnd {
        userFromSession { user ⇒
          entity(as[RequestParams.CreateReport]) { create ⇒
            val cmd = ReportManager.CreateReport(user.login, create.name)
            onSuccess((users ? cmd).mapTo[Result[Unit]]) {
              case Right(()) ⇒
                complete(ok())
              case Left(t: Throwable) ⇒
                complete(error(t))
            }
          }
        }
      }
    }
  }

  @Path("/{reportId}")
  @ApiOperation(
    value      = "更新一个report",
    notes      = "对报表进行重命名操作",
    httpMethod = "PUT")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "reportId",
        paramType = "path",
        dataType  = "java.util.UUID",
        required  = true),
      new ApiImplicitParam(
        paramType = "body",
        dataType  = "com.souo.biplatform.routes.designer.RequestParams$UpdateReport",
        required  = true)))
  @ApiResponses(
    Array(
      new ApiResponse(
        code    = 200,
        message = "成功")))
  def updateReport: Route = {
    (put & pathEnd) {
      userFromSession { user ⇒
        entity(as[RequestParams.UpdateReport]) { update ⇒
          val cmd = ReportManager.UpdateReport(user.login, update.id, update.name)
          onSuccess((users ? cmd).mapTo[Result[Unit]]) {
            case Right(_) ⇒
              complete(ok())
            case Left(t) ⇒
              complete(error(t))
          }
        }
      }
    }
  }

  @Path("/{reportId}")
  @ApiOperation(
    value      = "删除一个report",
    httpMethod = "DELETE")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "reportId",
        paramType = "path",
        dataType  = "java.util.UUID",
        required  = true)))
  @ApiResponses(
    Array(
      new ApiResponse(
        code    = 200,
        message = "成功")))
  def deleteReport: Route = {
    (delete & path(JavaUUID)) { id ⇒
      userFromSession { user ⇒
        val cmd = ReportManager.DropReport(user.login, id)
        onSuccess((users ? cmd).mapTo[Result[Unit]]) {
          case Right(_) ⇒
            complete(ok())
          case Left(t) ⇒
            complete(error(t))
        }
      }
    }
  }
}
