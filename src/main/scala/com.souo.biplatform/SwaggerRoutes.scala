package com.souo.biplatform

import akka.http.scaladsl.server.Route
import com.souo.biplatform.common.api.SwaggerSupport
import com.souo.biplatform.model.Swagger.{Contact, Info}
import com.souo.biplatform.routes.user.UsersRouters
import akka.http.scaladsl.server.Directives._
import com.souo.biplatform.routes.cube.CubeRoutes
import com.souo.biplatform.routes.designer.{DownLoadRoutes, ExecuteRoutes, ReportManagerRoutes, ReportsRoutes}
import com.souo.biplatform.routes.ds.DataSourceRoutes

import scala.reflect.runtime.universe._

/**
 * @author souo
 */
trait SwaggerRoutes extends SwaggerSupport {

  override val info = Info(
    description    = "biplatform",
    version        = "0.1",
    title          = "BI",
    termsOfService = "https://github.com/souo/biplatform",
    contact        = Some(
      Contact(
        name  = "souo",
        url   = "",
        email = "")))
  override val apiTypes: Seq[Type] = {
    Seq(
      typeOf[UsersRouters],
      typeOf[DataSourceRoutes],
      typeOf[CubeRoutes],
      typeOf[ReportManagerRoutes],
      typeOf[ReportsRoutes],
      typeOf[DownLoadRoutes],
      typeOf[ExecuteRoutes])
  }

  lazy val swagger: Route = {
    routes ~ get {
      pathEndOrSingleSlash{
        getFromResource("doc/index.html")
      }
    } ~ getFromResourceDirectory("doc")
  }

}
