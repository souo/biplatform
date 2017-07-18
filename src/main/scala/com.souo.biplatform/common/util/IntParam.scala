package com.souo.biplatform.common.util
import scala.util.Properties
/**
 * @author souo
 */
object IntParam {
  def unapply(str: String): Option[Int] = {
    try {
      Some(str.toInt)
    }
    catch {
      case e: NumberFormatException ⇒ None
    }
  }
}
