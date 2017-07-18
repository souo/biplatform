package com.souo.biplatform.cache

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.Duration

/**
 * @author souo
 */
trait LoggingSupport extends StrictLogging {

  /**
   * Output a debug log to record the result of a cache lookup
   */
  protected def logCacheHitOrMiss[A](key: String, result: Option[A]): Unit = {
    lazy val hitOrMiss = result.map(_ ⇒ "hit") getOrElse "miss"
    logger.debug(s"Cache $hitOrMiss for key $key")
  }

  /**
   * Output a debug log to record a cache insertion/update
   */
  protected def logCachePut(key: String, ttl: Option[Duration]): Unit = {
    lazy val ttlMsg = ttl.map(d ⇒ s" with TTL ${d.toMillis} ms") getOrElse ""
    logger.debug(s"Inserted value into cache with key $key$ttlMsg")
  }

}
