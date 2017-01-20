package com.souo.biplatform.admin

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Cookie, `Set-Cookie`}
import akka.http.scaladsl.server.Route
import com.souo.biplatform.common.api.Ok
import com.souo.biplatform.common.api.Response._
import com.souo.biplatform.system.CubeNode
import com.souo.biplatform.test.BaseRoutesSpec
import com.souo.biplatform.user.api.UsersRouters
import com.souo.biplatform.user.application.{UserRuleDao, UserService}
import io.circe.generic.auto._

import scala.language.postfixOps

/**
 * Created by souo on 2016/12/20
 */
class AdminRoutesSpecs extends BaseRoutesSpec {
  spec ⇒

  val userRuleDao = new UserRuleDao(spec.sqlDatabase)
  val userService = new UserService(userRuleDao)

  val dataSourceService = new MockDataSourceService()

  val routes = Route.seal(new AdminRoutes with TestRoutesSupport {
    override val dataSourceService: DataSourceService = spec.dataSourceService
    override val userService: UserService = spec.userService
    override val cubeNode: ActorRef = system.actorOf(CubeNode.props, CubeNode.name)
  }.adminRoutes)

  val userRoutes = Route.seal(new UsersRouters with TestRoutesSupport {
    override val userService: UserService = spec.userService
  }.usersRoutes)

  def withLoggedInUser(login: String)(body: RequestTransformer ⇒ Unit) = {
    Post("/users/login", Map("login" → login)) ~> userRoutes ~> check {
      status should be(StatusCodes.OK)
      val Some(sessionCookie) = header[`Set-Cookie`]
      body(addHeader(Cookie(sessionConfig.sessionCookieConfig.name, sessionCookie.cookie.value)))
    }
  }

  "Get /admin/tables with admin users login " should "get the right result" in {
    withLoggedInUser("admin") { transform ⇒
      Get("/admin/tables") ~> transform ~> routes ~> check {
        status should be(StatusCodes.OK)
        entityAs[Ok[List[TableInfo]]] should be(ok(List(TableInfo("test", "test", "测试"))))
      }
    }
  }

  "Get /admin/tables without login " should "reject by the service" in {
    Get("/admin/tables") ~> routes ~> check {
      status should be(StatusCodes.Forbidden)
    }
  }

  "Get /admin/tables with normal users  login " should "reject by the service" in {
    withLoggedInUser("user1") { transform ⇒
      Get("/admin/tables") ~> transform ~> routes ~> check {
        status should be(StatusCodes.Forbidden)
      }
    }
  }

}
