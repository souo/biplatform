package com.souo.biplatform.routes.designer

import java.util.UUID
import javax.ws.rs.Path

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.{ActorAttributes, Graph, SourceShape, Supervision}
import akka.util.ByteString
import com.souo.biplatform.common.api.{CirceSupport, Error, Response, SessionSupport}
import com.souo.biplatform.queryrouter.DataRow
import com.souo.biplatform.system.ReportNode
import io.circe.generic.auto._
import com.souo.biplatform.common.api.Response._
import com.souo.biplatform.model.Result
import io.swagger.annotations._

import scala.concurrent.duration._
import scala.util.control.NonFatal

/**
 * @author souo
 */
@Api(tags = Array("reports"))
@Path("/api/designer/reports")
trait ExecuteRoutes extends CirceSupport with SessionSupport {

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = {
    EntityStreamingSupport.json().withFramingRenderer(
      Flow[ByteString].intersperse(
        ByteString("{\"status\": 200, \"message\": \"OK\", \"data\":["),
        ByteString(","), ByteString("]}")))
  }
  val users: ActorRef

  @Path("/execute/{reportId}")
  @ApiOperation(
    value      = "执行一张报表",
    produces   = "application/json",
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
  def executeRoutes = (get & path("reports" / "execute" / Segment)) { id ⇒
    userFromSession { user ⇒
      val reportId = UUID.fromString(id)
      val cmd = ReportNode.Execute(user.login, reportId)
      onSuccess((users ? cmd)(60 seconds).mapTo[Result[Graph[SourceShape[DataRow], NotUsed]]]) {
        case Right(graph: Graph[SourceShape[DataRow], NotUsed]) ⇒
          val source: Source[DataRow, NotUsed] = Source.fromGraph(graph)
          complete(source)
        case Left(t) ⇒
          complete(error(t))
      }
    }
  }
}
