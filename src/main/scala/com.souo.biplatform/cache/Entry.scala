package com.souo.biplatform.cache

import org.joda.time.DateTime

case class Entry[+A](value: A, expiresAt: Option[DateTime]) {

  /**
   * Has the entry expired yet?
   */
  def isExpired: Boolean = expiresAt.exists(_.isBeforeNow)

}