package com.souo.biplatform.ds

import akka.actor.ActorRef
import akka.http.scaladsl.server.AuthorizationFailedRejection
import com.souo.biplatform.common.api.{DefaultTimeOut, RoutesSupport}
import com.souo.biplatform.user.api.SessionSupport
import com.typesafe.scalalogging.StrictLogging
import akka.http.scaladsl.server.Directives._
import com.souo.biplatform.common.api.Response._
import com.souo.biplatform.system.DataSourceNode
import com.souo.biplatform.user.application.UserService
import akka.pattern.ask
import com.souo.biplatform.model.DataSources
import io.circe.generic.auto._
import java.util.UUID

import com.souo.biplatform.system.DataSourceNode.Item

/**
 * Created by souo on 2017/1/24
 */
trait DataSourceRoutes extends RoutesSupport with StrictLogging with SessionSupport with DefaultTimeOut {

  val userService: UserService
  val dsNode: ActorRef

  val dsRoutes = pathPrefix("datasource") {
    //获取定义的所有 datasource
    (get & pathEnd) {
      userFromSession { user ⇒
        onSuccess(userService.isAdminUser(user)) {
          case true ⇒
            val cmd = DataSourceNode.ListAllDS
            onSuccess((dsNode ? cmd).mapTo[DataSourceNode.Items]) { res ⇒
              complete(ok(res))
            }
          case false ⇒
            logger.warn(s"login user ${user.login} is not allow access this resource")
            reject(AuthorizationFailedRejection)
        }
      }
    } ~ (post & path("csv")) {
      userFromSession { user ⇒
        onSuccess(userService.isAdminUser(user)) {
          case true ⇒
            entity(as[RequestParams.Csv]) { csv ⇒
              val ds = DataSources.csv(csv.path, csv.sep)
              val cmd = DataSourceNode.Add(csv.name, ds)
              onSuccess((dsNode ? cmd).mapTo[DataSourceNode.Item]) { res ⇒
                complete(ok(res))
              }
            }
          case false ⇒
            logger.warn(s"login user ${user.login} is not allow access this resource")
            reject(AuthorizationFailedRejection)
        }
      }
    } ~ (post & path("mysql")) {
      userFromSession { user ⇒
        onSuccess(userService.isAdminUser(user)) {
          case true ⇒
            entity(as[RequestParams.Mysql]) {
              case RequestParams.Mysql(name, host, port, db, user, pwd) ⇒
                val ds = DataSources.mysql(host, port, db, user, pwd)
                val cmd = DataSourceNode.Add(name, ds)
                onSuccess((dsNode ? cmd).mapTo[DataSourceNode.Item]) { res ⇒
                  complete(ok(res))
                }
            }
          case false ⇒
            logger.warn(s"login user ${user.login} is not allow access this resource")
            reject(AuthorizationFailedRejection)
        }
      }
    } ~ (get & path(Segment)) { id ⇒
      val dsId = UUID.fromString(id)
      userFromSession { user ⇒
        onSuccess(userService.isAdminUser(user)) {
          case true ⇒
            val cmd = DataSourceNode.Get(dsId)
            val f = (dsNode ? cmd).mapTo[Option[Item]].map {
              _.map(_.dataSource.listAllTables())
            }
            onSuccess(f) {
              case Some(Left(t)) ⇒
                complete(error(t))
              case Some(Right(r)) ⇒
                complete(ok(r))
              case None ⇒
                complete(error(msg = "no such datasource"))
            }
          case false ⇒
            logger.warn(s"login user ${user.login} is not allow access this resource")
            reject(AuthorizationFailedRejection)
        }
      }
    } ~ (get & path(Segment / Segment)) { (did, tid) ⇒
      val dsId = UUID.fromString(did)
      userFromSession { user ⇒
        onSuccess(userService.isAdminUser(user)) {
          case true ⇒
            val cmd = DataSourceNode.Get(dsId)
            val f = (dsNode ? cmd).mapTo[Option[Item]].map {
              _.map(_.dataSource.listAllColumns(tid))
            }
            onSuccess(f) {
              case Some(Left(t)) ⇒
                complete(error(t))
              case Some(Right(r)) ⇒
                complete(ok(r))
              case None ⇒
                complete(error(msg = "no such datasource"))
            }
          case false ⇒
            logger.warn(s"login user ${user.login} is not allow access this resource")
            reject(AuthorizationFailedRejection)
        }
      }
    } ~ (delete & path(Segment)) { id ⇒
      val dsId = UUID.fromString(id)
      userFromSession { user ⇒
        onSuccess(userService.isAdminUser(user)) {
          case true ⇒
            val cmd = DataSourceNode.Remove(dsId)
            onSuccess(dsNode ? cmd) {
              case DataSourceNode.NoSuchDS ⇒
                complete(error(msg = "no such datasource"))
              case _ ⇒
                complete(ok())
            }
          case false ⇒
            logger.warn(s"login user ${user.login} is not allow access this resource")
            reject(AuthorizationFailedRejection)
        }
      }
    }
  }

}
