package com.souo.biplatform.designer

import akka.http.scaladsl.server.Directives._

/**
 * Created by souo on 2017/1/20
 */
trait DesignerRoutes extends ReportsRoutes
    with ExecuteRoutes
    with DownLoadRoutes {

  val designerRoutes = pathPrefix("designer") {
    reportsRoutes ~
      executeRoutes ~
      downLoadRoutes
  }
}
