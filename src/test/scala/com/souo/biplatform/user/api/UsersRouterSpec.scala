package com.souo.biplatform.user.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.souo.biplatform.common.api.Ok
import com.souo.biplatform.common.api.Response._
import com.souo.biplatform.test.BaseRoutesSpec
import com.souo.biplatform.user.application.{UserRuleDao, UserService}
import io.circe.generic.auto._

/**
 * Created by souo on 2016/12/19
 */
class UsersRouterSpec extends BaseRoutesSpec {
  spec ⇒

  val userRuleDao = new UserRuleDao(spec.sqlDatabase)
  val userService = new UserService(userRuleDao)

  val routes = Route.seal(new UsersRouters with TestRoutesSupport {
    override val userService: UserService = spec.userService
  }.usersRoutes)

  "POST /users/login with admin user " should "login as admin" in {
    Post("/users/login", Map("login" → "admin")) ~> routes ~> check {
      status should be (StatusCodes.OK)
      entityAs[Ok[Map[String, Boolean]]] should be (ok(Map("isAdmin" → true)))
    }
  }

  "POST /users/login with normal user " should "login is not admin" in {
    Post("/users/login", Map("login" → "other")) ~> routes ~> check {
      status should be (StatusCodes.OK)
      entityAs[Ok[Map[String, Boolean]]] should be (ok(Map("isAdmin" → false)))
    }
  }

}
