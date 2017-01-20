package com.souo.biplatform.designer

import java.util.UUID

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshalling.{Marshaller, Marshalling}
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.{Graph, SourceShape}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.souo.biplatform.common.api.CirceSupport
import com.souo.biplatform.model.QueryModel
import com.souo.biplatform.queryrouter.{DataCell, DataRow, DataSet}
import com.souo.biplatform.user.api.SessionSupport
import com.souo.biplatform.queryrouter.DataCellType._
import com.souo.biplatform.system.ReportNode
import io.circe.generic.auto._

import scala.concurrent.duration._

/**
 * Created by souo on 2017/1/18
 */
trait DownLoadRoutes extends CirceSupport with SessionSupport {

  val users: ActorRef

  // [1] provide a marshaller to ByteString
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
      ByteString(values.mkString(","))
    })
  }

  // [2] enable csv streaming:
  implicit val csvStreaming = EntityStreamingSupport.csv()

  val downLoadRoutes = (post & path("designer" / "reports" / "download" / Segment)) { id ⇒
    userFromSession { user ⇒
      val reportId = UUID.fromString(id)
      entity(as[QueryModel]) { query ⇒
        val cmd = ReportNode.Execute(user.login, reportId, query)
        onSuccess((users ? cmd) (60 seconds).mapTo[Graph[SourceShape[DataRow], NotUsed]]) { graph ⇒
          val source: Source[DataRow, NotUsed] = Source.fromGraph(graph)
          complete(source)
        }
      }
    }
  }
}
