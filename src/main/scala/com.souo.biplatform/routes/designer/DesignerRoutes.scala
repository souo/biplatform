package com.souo.biplatform.routes.designer

import akka.http.scaladsl.server.Directives._

/**
 * @author souo
 */
trait DesignerRoutes extends ReportsRoutes
    with ExecuteRoutes
    with DownLoadRoutes
    with DashBoardRoutes
    with ReportManagerRoutes {

  val designerRoutes = pathPrefix("designer") {
    reportManagerRoutes ~
      reportsRoutes ~
      dashBoardRoutes ~
      executeRoutes ~
      downLoadRoutes
  }
}
