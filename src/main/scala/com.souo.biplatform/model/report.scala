package com.souo.biplatform.model

import java.util.UUID

import cats.data._
import org.joda.time.DateTime

/**
 * Created by souo on 2016/12/20
 */

case class ReportMeta(
  id:            UUID,
  name:          String,
  createBy:      String,
  createTime:    DateTime,
  modifyBy:      Option[String],
  latModifyTime: Option[DateTime]
) extends Serializable

case class QueryModel(
    dimensions: List[Dimension],
    measures:   List[Measure],
    filters:    Option[List[Filter]] = None
) {
  def check(): Validated[String, Boolean] = {
    if (dimensions.isEmpty || measures.isEmpty) {
      Validated.invalid("至少指定一个维度和一个指标")
    }
    else {
      Validated.valid(true)
    }
  }
}

case class ReportQuery(
  queryModel: QueryModel,
  cubeId:     UUID,
  properties: Map[String, String]
)

