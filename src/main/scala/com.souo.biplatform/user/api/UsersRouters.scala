package com.souo.biplatform.user.api

import akka.http.scaladsl.server.Directives._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.common.api.RoutesSupport
import com.souo.biplatform.common.api.Response._
import com.souo.biplatform.user.application.UserService
import com.souo.biplatform.user.{Session, User}
import io.circe.generic.auto._

/**
 * @author souo
 */
trait UsersRouters extends RoutesSupport with StrictLogging with SessionSupport {

  val userService: UserService

  val usersRoutes = pathPrefix("users") {
    path("logout") {
      get {
        userFromSession { user ⇒
          logger.info(s"${user.login} logout")
          invalidateSession(oneOff, usingCookies) {
            complete(ok())
          }
        }
      }
    } ~ path("login"){
      post{
        entity(as[User]) { user ⇒
          onSuccess(userService.isAdminUser(user)){ isAdmin ⇒
            val rule = if (isAdmin) "admin"; else "normal"
            logger.info(s"${user.login} login as $rule")
            val session = Session(user)
            setSession(oneOff, usingCookies, session) {
              complete(ok(Map("isAdmin" → isAdmin)))
            }
          }
        }
      }
    }
  }

}
