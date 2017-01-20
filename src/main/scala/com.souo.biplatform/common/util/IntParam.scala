package com.souo.biplatform.common.util

import cats.syntax.either._

/**
 * Created by souo on 2017/1/3
 */
object IntParam {
  def unapply(str: String): Option[Int] = {
    Either.catchNonFatal(str.toInt).toOption
  }
}
