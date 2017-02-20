package com.souo.biplatform.designer

import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import cats.data.Validated.{Invalid, Valid}
import com.souo.biplatform.common.api.{DefaultTimeOut, RoutesSupport}
import com.souo.biplatform.model.{QueryModel, ReportMeta, ReportQuery}
import com.souo.biplatform.queryrouter.DataSet
import com.souo.biplatform.system.{ReportNode, UserNode}
import com.souo.biplatform.user.api.SessionSupport
import com.souo.biplatform.common.api.Response._
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.auto._

import scala.concurrent.duration._

/**
 * Created by souo on 2016/12/26
 */
trait ReportsRoutes extends RoutesSupport with StrictLogging with SessionSupport with DefaultTimeOut {

  val users: ActorRef

  val reportsRoutes = pathPrefix("reports") {
    (get & pathEnd) {
      userFromSession { user ⇒
        val cmd = UserNode.ListAllReport(user.login)
        onSuccess(users ? cmd) {
          case reports: Array[ReportMeta] ⇒
            complete(ok(reports))
        }
      }
    } ~ (post & pathEnd) {
      userFromSession { user ⇒
        entity(as[RequestParams.CreateReport]) { create ⇒
          val cmd = UserNode.CreateReport(user.login, create.name)
          onSuccess(users ? cmd) {
            case Right(metas: Array[ReportMeta]) ⇒
              complete(ok(metas))
            case Left(t: Throwable) ⇒
              complete(error(t))
          }
        }
      }
    } ~ (put & pathEnd) {
      userFromSession { user ⇒
        entity(as[RequestParams.UpdateReport]) { update ⇒
          val cmd = UserNode.UpdateReport(user.login, update.id, update.name)
          onSuccess((users ? cmd).mapTo[Either[Throwable, Array[ReportMeta]]]) {
            case Right(metaList) ⇒
              complete(ok(metaList))
            case Left(t) ⇒
              complete(error(t))
          }
        }
      }
    } ~ (delete & pathEnd) {
      userFromSession { user ⇒
        parameters('reportId) { id ⇒
          val cmd = UserNode.DropReport(user.login, UUID.fromString(id))
          onSuccess((users ? cmd).mapTo[Either[Throwable, Array[ReportMeta]]]) {
            case Right(metaList) ⇒
              complete(ok(metaList))
            case Left(t) ⇒
              complete(error(t))
          }
        }
      }
    } ~ (get & path(Segment)) { id ⇒
      userFromSession { user ⇒
        val reportId = UUID.fromString(id)
        val cmd = ReportNode.Show(user.login, reportId)
        onSuccess((users ? cmd).mapTo[Option[ReportQuery]]) {
          case Some(r) ⇒
            complete(ok[ReportQuery](r))
          case None ⇒ complete(ok())
        }
      }
    } ~ (post & path(Segment)) { id ⇒
      userFromSession { user ⇒
        val reportId = UUID.fromString(id)
        entity(as[ReportQuery]) { query ⇒
          val cmd = ReportNode.Save(user.login, reportId, query)
          onSuccess(users ? cmd) {
            case ReportNode.SaveSucceed ⇒
              complete(ok())
            case ReportNode.SaveFailure(t) ⇒
              complete(error(t))
          }
        }
      }
    } ~ (delete & path(Segment)) { id ⇒
      userFromSession { user ⇒
        val reportId = UUID.fromString(id)
        val cmd = ReportNode.Clear(user.login, reportId)
        onSuccess(users ? cmd) {
          case ReportNode.DeleteSucceed ⇒
            complete(ok())
          case ReportNode.DeleteFailure(t) ⇒
            complete(error(t))
        }
      }
    }
  }
}
