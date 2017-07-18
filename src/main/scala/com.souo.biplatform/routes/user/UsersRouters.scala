package com.souo.biplatform.routes.user

import javax.ws.rs.Path

import akka.actor.ActorPath
import akka.http.scaladsl.server.Directives._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.common.api.Response._
import com.souo.biplatform.common.api.{RoutesSupport, SessionSupport}
import com.souo.biplatform.model.{Session, User, UserWithRole}
import io.circe.generic.auto._
import io.swagger.annotations._

/**
 * @author souo
 */
@Api(tags = Array("user"), produces = "application/json")
@Path("/api/users")
trait UsersRouters extends RoutesSupport with StrictLogging with SessionSupport {

  val userService: UserService

  val usersRoutes = pathPrefix("users") {
    login ~ logout
  }

  @Path("/login")
  @ApiOperation(
    value           = "登入",
    httpMethod      = "POST",
    consumes        = "application/json",
    responseHeaders = Array(
      new ResponseHeader(name = "Set-Cookie", description = "set an cookie")
    )
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        value     = "",
        paramType = "body",
        dataType  = "com.souo.biplatform.model.User",
        required  = true
      )
    )
  )
  @ApiResponses(
    Array(
      new ApiResponse(
        code    = 200,
        message = "成功"
      ),
      new ApiResponse(
        code    = 500,
        message = "失败"
      )
    )
  )
  def login = path("login") {
    post {
      entity(as[User]) { user ⇒
        if (ActorPath.isValidPathElement(user.login)) {
          onSuccess(userService.isAdminUser(user)) { isAdmin ⇒
            val rule = if (isAdmin) "admin"; else "normal"
            logger.info(s"${user.login} login as $rule")
            val session = Session(UserWithRole(user.login, isAdmin))
            setSession(oneOff, usingCookies, session) {
              complete(ok(Map("isAdmin" → isAdmin)))
            }
          }
        }
        else {
          failWith(new RuntimeException("不合法的登录用户名"))
        }
      }
    }
  }

  @Path("/logout")
  @ApiOperation(value = "登出", httpMethod = "GET")
  def logout = path("logout") {
    get {
      userFromSession { user ⇒
        logger.info(s"${user.login} logout")
        invalidateSession(oneOff, usingCookies) {
          complete(ok())
        }
      }
    }
  }
}
