package com.souo.biplatform.model

import java.util.UUID

import cats.data._
import org.joda.time.DateTime
import cats.implicits._

/**
 * @author souo
 */

case class ReportMeta(
  id:              UUID,
  name:            String,
  createBy:        String,
  createTime:      DateTime,
  lastEditTime:    Option[DateTime] = None,
  lastPublishTime: Option[DateTime] = None,
  edited:          Boolean          = false,
  published:       Boolean          = false) extends Serializable {

  def edit: ReportMeta = copy(
    lastEditTime = Some(DateTime.now()),
    edited       = true)

  def publish: ReportMeta = copy(
    lastPublishTime = Some(DateTime.now()),
    published       = true)

  def reName(name: String): ReportMeta = copy(name = name)

  def clean: ReportMeta = copy(
    lastEditTime = Some(DateTime.now()),
    edited       = true,
    published    = false)

}

object ReportMeta {
  def create(name: String, createBy: String): ReportMeta = {
    ReportMeta(
      id         = UUID.randomUUID(),
      name       = name,
      createBy   = createBy,
      createTime = DateTime.now())
  }
}

case class QueryModel(
  tableName:  String,
  dimensions: List[Dimension],
  measures:   List[Measure],
  filters:    Option[List[Filter]] = None) {

  def checkTableName = {
    if (tableName.isEmpty) {
      Validated.invalid("表名不能为空")
    }
    else {
      Validated.valid(true)
    }
  }.toValidatedNel

  def checkDimensionsAndMeasures = {
    if (dimensions.isEmpty || measures.isEmpty) {
      Validated.invalid("至少指定一个维度和一个指标")
    }
    else {
      Validated.valid(true)
    }
  }.toValidatedNel

  def check(): ValidatedNel[String, Boolean] = {
    checkTableName |+| checkDimensionsAndMeasures
  }
}

case class ReportQuery(
  queryModel: QueryModel,
  cubeId:     UUID,
  properties: Map[String, String])

