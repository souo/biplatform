package com.souo.biplatform.designer

import java.util.UUID

/**
 * Created by souo on 2016/12/26
 */
object RequestParams {

  case class CreateReport(name: String)
  case class UpdateReport(id: UUID, name: String)
  case class DeleteReport(id: UUID)

}
