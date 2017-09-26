package com.souo.biplatform.routes.designer

import java.util.UUID
import javax.ws.rs.Path

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.common.api.{DefaultTimeOut, RoutesSupport, SessionSupport}
import com.souo.biplatform.common.api.Response._
import com.souo.biplatform.model.{ReportQuery, Result}
import com.souo.biplatform.system.ReportNode
import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import io.swagger.annotations._

/**
 * @author souo
 */
@Api(tags     = Array("reports"), produces = "application/json")
@Path("/api/designer/reports")
trait ReportsRoutes extends RoutesSupport with StrictLogging with SessionSupport with DefaultTimeOut {

  val users: ActorRef

  val reportsRoutes = pathPrefix("reports") {
    showReport ~
      publishReport ~
      saveReportQuery ~
      cleanReport ~
      getDimValues ~
      modifyEetendArea
  }

  @Path("/{reportId}")
  @ApiOperation(
    value      = "获取一个report",
    notes      = "获取一张报表最后一次保存的状态",
    httpMethod = "GET")
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
        code     = 200,
        message  = "成功",
        response = classOf[ReportQuery])))
  def showReport: Route = {
    get {
      path(Segment){ id ⇒
        userFromSession { user ⇒
          val reportId = UUID.fromString(id)
          val cmd = ReportNode.Show(user.login, reportId)
          onSuccess((users ? cmd).mapTo[Option[ReportQuery]]) {
            case Some(r) ⇒
              complete(ok[ReportQuery](r))
            case None ⇒ complete(ok())
          }
        }
      }
    }
  }

  @Path("/publish/{reportId}")
  @ApiOperation(
    value      = "发布一张report",
    notes      = "发布一张报表要求，报表为可发布状态。即报表必须已编辑，而且至少包含一组维度和指标",
    httpMethod = "GET")
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
  def publishReport: Route = {
    get {
      path("publish" / Segment){ id ⇒
        userFromSession { user ⇒
          val reportId = UUID.fromString(id)
          val cmd = ReportNode.Publish(user.login, reportId)
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
    value      = "保存report",
    notes      = "保存页面的最新发布状态",
    httpMethod = "POST")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "reportId",
        paramType = "path",
        dataType  = "java.util.UUID",
        required  = true),
      new ApiImplicitParam(
        paramType = "body",
        dataType  = "com.souo.biplatform.model.ReportQuery",
        required  = true)))
  @ApiResponses(
    Array(
      new ApiResponse(
        code    = 200,
        message = "成功")))
  def saveReportQuery: Route = {
    post {
      path(Segment){ id ⇒
        userFromSession { user ⇒
          val reportId = UUID.fromString(id)
          entity(as[ReportQuery]) { query ⇒
            val cmd = ReportNode.Save(user.login, reportId, query)
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
  }

  @Path("/{reportId}/queryModel")
  @ApiOperation(
    value      = "删除report状态",
    notes      = "删除以保存的所有状态",
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
  def cleanReport: Route = {
    (delete & path(JavaUUID / "queryModel")) { id ⇒
      userFromSession { user ⇒
        val cmd = ReportNode.Clear(user.login, id)
        onSuccess((users ? cmd).mapTo[Result[Unit]]) {
          case Right(_) ⇒
            complete(ok())
          case Left(t) ⇒
            complete(error(t))
        }
      }
    }
  }

  @Path("/{reportId}/dim/{{dimName}}/values")
  @ApiOperation(
    value      = "获取某个维度所有值",
    notes      = "需先定义维度和指标",
    httpMethod = "GET")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "reportId",
        paramType = "path",
        dataType  = "java.util.UUID",
        required  = true),
      new ApiImplicitParam(
        name      = "dimName",
        paramType = "path",
        dataType  = "string",
        required  = true)))
  @ApiResponses(
    Array(
      new ApiResponse(
        code              = 200,
        message           = "成功",
        response          = classOf[String],
        responseContainer = "List")))
  def getDimValues: Route = {
    (get & path(JavaUUID / "dim" / Segment / "values")) { (id, dimName) ⇒
      userFromSession { user ⇒
        val cmd = ReportNode.GetDimValues(user.login, id, dimName)
        onSuccess((users ? cmd).mapTo[Result[List[String]]]) {
          case Right(r) ⇒
            complete(ok(r))
          case Left(t) ⇒
            complete(error(t))
        }
      }
    }
  }

  def modifyEetendArea = {
    post {
      path(JavaUUID / "extendArea") { id ⇒
        userFromSession { user ⇒
          entity(as[Map[String, String]]){ props ⇒
            val cmd = ReportNode.UpdateExtendArea(user.login, id, props)
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
  }
}
