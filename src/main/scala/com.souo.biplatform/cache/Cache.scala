package com.souo.biplatform.cache

import com.souo.biplatform.cache.serializer.Codec

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
 * @author souo
 */
trait Cache[Repr] {

  def get[V](key: String)(implicit codec: Codec[V, Repr]): Future[Option[V]]

  def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V, Repr]): Future[Unit]

  def remove(key: String): Future[Unit]

  /**
   * Delete the entire contents of the cache. Use wisely!
   */
  def removeAll(): Future[Unit]

  def close(): Unit
}
