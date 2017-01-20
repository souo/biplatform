package com.souo.biplatform.common.api

/**
 * @author souo
 */
sealed trait Response

case class Ok[T](status: Int = 200, message: String = "OK", data: T) extends Response

case class Error(status: Int = 5000, message: String = "错误", moreInfo: String) extends Response

object Response {

  def ok[T](res: T) = Ok[T](data = res)

  def ok() = Ok(data = None)

  def error(msg: String) = Error(moreInfo = msg)

  def error(t: Throwable) = Error(moreInfo = t.getMessage)

  def notFound(msg: String) = Error(status = 5004, message = "Not Found", moreInfo = msg)
}

