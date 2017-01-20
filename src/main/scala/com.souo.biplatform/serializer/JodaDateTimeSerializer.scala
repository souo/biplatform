package com.souo.biplatform.serializer

import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.{Kryo, Serializer}
import org.joda.time.{DateTime, DateTimeZone}

/**
 * Created by souo on 2017/1/1
 */
class JodaDateTimeSerializer extends Serializer[DateTime] {
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
