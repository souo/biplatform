package com.souo.biplatform.serializer

import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.{Kryo, Serializer}
import org.joda.time.{DateTime, DateTimeZone}
import com.twitter.chill.{IKryoRegistrar, toRich}

/**
 * @author souo
 */
class JodaDateTimeSerializer extends Serializer[DateTime] with IKryoRegistrar {

  override def apply(kryo: Kryo): Unit = {
    if (!kryo.alreadyRegistered(classOf[DateTime])) {
      kryo.forClass[DateTime](this)
      kryo.forSubclass[DateTime](this)
    }
  }

  override def write(kryo: Kryo, output: Output, t: DateTime): Unit = {
    output.writeLong(t.getMillis, true)
    output.writeString(t.getZone.getID)
  }

  override def read(kryo: Kryo, input: Input, aClass: Class[DateTime]): DateTime = {
    val millis = input.readLong(true)
    val zone = DateTimeZone.forID(input.readString())
    new DateTime(millis, zone)
  }
}
