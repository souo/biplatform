package com.souo.biplatform.common.api

import akka.util.Timeout
import scala.concurrent.duration._
/**
 * @author souo
 */
trait DefaultTimeOut {
  implicit val timeOut = Timeout(10 seconds)
}
