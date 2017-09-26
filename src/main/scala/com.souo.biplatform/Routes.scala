package com.souo.biplatform

import akka.http.scaladsl.server.Directives._
import com.souo.biplatform.common.api.RoutesRequestWrapper
import com.souo.biplatform.routes.cube.CubeRoutes
import com.souo.biplatform.routes.designer.DesignerRoutes
import com.souo.biplatform.routes.ds.DataSourceRoutes
import com.souo.biplatform.routes.user.UsersRouters

/**
 * @author souo
 */
trait Routes extends RoutesRequestWrapper
  with UsersRouters
  with CubeRoutes
  with DataSourceRoutes
  with DesignerRoutes {

  lazy val api = requestWrapper {
    pathPrefix("api") {
      usersRoutes ~
        cubeRoutes ~
        dsRoutes ~
        designerRoutes
    }
  }
}
