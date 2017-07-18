package com.souo.biplatform.cache.serializer

/**
 * @author souo
 */
trait Codec[From, To] {
  def serialize(value: From): To
  def deserialize(data: To): From
}
