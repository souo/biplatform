package com.souo.biplatform.cache
package guava

import org.joda.time.DateTime
import com.google.common.cache.{Cache ⇒ GCache, CacheBuilder ⇒ GCacheBuilder}
import com.souo.biplatform.cache.serializer.Codec

import scala.concurrent.duration.Duration
import scala.concurrent.Future

class GuavaCache[InMemoryRepr](underlying: GCache[String, Object])
  extends Cache[InMemoryRepr]
  with LoggingSupport {

  override def get[V](key: String)(implicit codec: Codec[V, InMemoryRepr]) = {
    val baseValue = underlying.getIfPresent(key)
    val result = {
      if (baseValue != null) {
        val entry = baseValue.asInstanceOf[Entry[V]]
        if (entry.isExpired) {
          remove(key)
          None
        }
        else {
          Some(entry.value)
        }
      }
      else {
        None
      }
    }
    logCacheHitOrMiss(key, result)
    Future.successful(result)
  }

  override def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V, InMemoryRepr]) = {
    val entry = Entry(value, ttl.map(toExpiryTime))
    underlying.put(key, entry.asInstanceOf[Object])
    logCachePut(key, ttl)
    Future.successful(())
  }

  override def remove(key: String): Future[Unit] = {
    Future.successful(underlying.invalidate(key))
  }
  /**
   * Delete the entire contents of the cache. Use wisely!
   */
  override def removeAll(): Future[Unit] = {
    Future.successful(underlying.invalidateAll())
  }

  override def close(): Unit = {
    // Nothing to do
  }

  private def toExpiryTime(ttl: Duration): DateTime = DateTime.now.plus(ttl.toMillis)

}

object GuavaCache {

  /**
   * Create a new Guava cache
   */
  def apply[T](): GuavaCache[T] = apply[T](GCacheBuilder.newBuilder().build[String, Object]())

  /**
   * Create a new cache utilizing the given underlying Guava cache.
   */
  def apply[T](underlying: GCache[String, Object]): GuavaCache[T] = new GuavaCache[T](underlying)

}
