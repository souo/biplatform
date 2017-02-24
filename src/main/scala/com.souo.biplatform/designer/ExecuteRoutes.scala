package com.souo.biplatform.designer

import java.util.UUID

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.{Graph, SourceShape}
import akka.stream.scaladsl.Source
import com.souo.biplatform.common.api.CirceSupport
import com.souo.biplatform.model.QueryModel
import com.souo.biplatform.queryrouter.DataRow
import com.souo.biplatform.user.api.SessionSupport
import com.souo.biplatform.system.ReportNode
import io.circe.generic.auto._

import scala.concurrent.duration._



/**
 * Created by souo on 2017/1/20
 */
trait ExecuteRoutes extends CirceSupport with SessionSupport {

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  val users: ActorRef

  val executeRoutes = (post & path("reports" / Segment / "execute")) { id ⇒
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
