package com.souo.biplatform.cache.serializer

import com.twitter.chill.KryoInjection

/**
 * @author souo
 */
trait KryoSerializationCodec {
  implicit def kryoBinaryCodec = new KryoSerializationAnyCodec
}

class KryoSerializationAnyCodec extends Codec[Any, Array[Byte]] {
  override def serialize(value: Any): Array[Byte] = {
    KryoInjection.apply(value)
  }

  override def deserialize(data: Array[Byte]): Any = {
    KryoInjection.invert(data)
  }
}

object KryoSerializationCodec extends KryoSerializationCodec

