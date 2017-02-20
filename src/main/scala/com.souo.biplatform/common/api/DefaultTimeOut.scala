package com.souo.biplatform.common.api

import akka.util.Timeout
import scala.concurrent.duration._

/**
 * Created by souo on 2017/1/24
 */
trait DefaultTimeOut {
  implicit val timeOut = Timeout(10 seconds)
}
