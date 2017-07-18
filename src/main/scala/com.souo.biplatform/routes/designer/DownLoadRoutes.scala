package com.souo.biplatform.routes.designer

import java.util.UUID
import javax.ws.rs.Path

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshalling.{Marshaller, Marshalling}
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.{Graph, SourceShape}
import akka.util.ByteString
import com.souo.biplatform.common.api.{CirceSupport, Error, Response, SessionSupport}
import com.souo.biplatform.common.api.Response.error
import com.souo.biplatform.model.{CubeSchema, QueryModel, Result}
import com.souo.biplatform.queryrouter.{DataCell, DataRow}
import com.souo.biplatform.queryrouter.DataCellType._
import com.souo.biplatform.system.{ReportNode, UserNode}
import io.circe.generic.auto._
import io.swagger.annotations._

import scala.concurrent.duration._
import com.souo.biplatform.common.csv._

/**
 * @author souo
 */
@Api(tags = Array("reports"))
@Path("/api/designer/reports")
trait DownLoadRoutes extends CirceSupport with SessionSupport {

  val users: ActorRef

  val BOM: ByteString = ByteString.fromArray(
    Array(
      0xEF,
      0xBB,
      0xBF
    ).map(_.toByte)
  )

  val csvPrinter = new CSVPrinter()

  implicit val dataRowAsCsv = Marshaller.strict[DataRow, ByteString] { r ⇒
    Marshalling.WithFixedContentType(ContentTypes.`text/csv(UTF-8)`, () ⇒ {
      val values = r.cells.map {
        case DataCell(value, ROW_HEADER_HEADER, props) ⇒
          Option(props).flatMap(_.get("caption")).filter(_.nonEmpty).getOrElse(value)
        case DataCell(value, COLUMN_HEADER, props) ⇒
          Option(props).flatMap(_.get("caption")).filter(_.nonEmpty).getOrElse(value)
        case c ⇒
          c.value
      }
      val lines = csvPrinter.printRow(values)
      ByteString(lines)
    })
  }

  implicit val csvStreaming = EntityStreamingSupport.csv().withFramingRenderer{
    Flow[ByteString].intersperse(
      BOM,
      ByteString("\n"),
      ByteString.empty
    )
  }

  @Path("/download/{reportId}")
  @ApiOperation(
    value      = "下载一张报表",
    produces   = "text/csv(UTF-8)",
    httpMethod = "GET"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "reportId",
        paramType = "path",
        dataType  = "java.util.UUID",
        required  = true
      )
    )
  )
  @ApiResponses(
    Array(
      new ApiResponse(
        code    = 200,
        message = "成功"
      )
    )
  )
  def downLoadRoutes = (get & path("reports" / "download" / Segment)) { id ⇒
    userFromSession { user ⇒
      val reportId = UUID.fromString(id)
      val cmd = ReportNode.Execute(user.login, reportId)
      onSuccess((users ? cmd) (60 seconds).mapTo[Result[Graph[SourceShape[DataRow], NotUsed]]]) {
        case Right(graph: Graph[SourceShape[DataRow], NotUsed]) ⇒
          val source: Source[DataRow, NotUsed] = Source.fromGraph(graph)
          complete(source)
        case Left(t) ⇒
          complete(error(t))
      }
    }
  }
}
