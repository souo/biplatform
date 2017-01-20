package com.souo.biplatform

import akka.http.scaladsl.server.Directives._
import com.souo.biplatform.admin.AdminRoutes
import com.souo.biplatform.designer.DesignerRoutes
import com.souo.biplatform.common.api.RoutesRequestWrapper
import com.souo.biplatform.designer.{DownLoadRoutes, ReportsRoutes}
import com.souo.biplatform.user.api.UsersRouters

/**
 * @author souo
 */
trait Routes extends RoutesRequestWrapper
    with UsersRouters
    with AdminRoutes
    with DesignerRoutes {

  lazy val routes = requestWrapper {
    pathPrefix("api") {
      usersRoutes ~
        adminRoutes ~
        designerRoutes
    }
  }
}
