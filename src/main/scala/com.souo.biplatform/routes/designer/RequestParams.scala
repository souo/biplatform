package com.souo.biplatform.routes.designer

import java.util.UUID

/**
 * @author souo
 */
object RequestParams {
  case class CreateReport(name: String)
  case class UpdateReport(id: UUID, name: String)
  case class DeleteReport(id: UUID)

  case class CreateDashBoard(dashboardName: String)
  case class RenameDashBoard(dashBoardId: UUID, dashboardName: String)
  case class DropDashBoard(dashBoardId: UUID)

  case class AddWidget(
    reportId:          UUID,
    visualizationType: String,
    size:              String
  )

}
