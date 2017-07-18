package com.souo.biplatform.common.api

import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

/**
 * @author souo
 */
sealed trait Response

@ApiModel
case class Ok[T](
  @(ApiModelProperty @field)(value = "业务状态码", dataType = "int",
    required        = true, allowableValues = "200") status: Int = 200,
  @(ApiModelProperty @field)(value = "提示消息", dataType = "string", required = true) message:String= "OK",
  @(ApiModelProperty @field)(value = "结果字段", dataType = "object") data:            T
) extends Response

@ApiModel
case class Error(
  @(ApiModelProperty @field)(
    value           = "业务状态码",
    dataType        = "int", required = true, allowableValues = "5000"
  ) status: Int = 5000,
  @(ApiModelProperty @field)(value = "提示消息", dataType = "string", required = true) message:String= "错误",
  @(ApiModelProperty @field)(value = "错误描述", dataType = "string") moreInfo:        String
) extends Response

object Response {

  def ok[T](res: T) = Ok[T](data = res)

  def ok() = Ok(data = None)

  def errorMsg(msg: String) = Error(moreInfo = msg)

  def error(t: Throwable) = Error(moreInfo = t.getMessage)

  def notFound(msg: String) = Error(status = 5004, message = "Not Found", moreInfo = msg)
}

