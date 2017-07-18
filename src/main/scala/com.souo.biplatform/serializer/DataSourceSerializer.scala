package com.souo.biplatform.serializer

import com.souo.biplatform.model.{DataSource, JdbcSource}
import com.esotericsoftware.kryo.{Kryo, Serializer}
import com.esotericsoftware.kryo.io.{Input, Output}
import com.twitter.chill.{toRich, IKryoRegistrar}

/**
 * @author souo
 */
class DataSourceSerializer extends Serializer[DataSource] with IKryoRegistrar {

  override def apply(kryo: Kryo): Unit = {
    if (!kryo.alreadyRegistered(classOf[DataSource])) {
      kryo.forClass[DataSource](this)
      kryo.forSubclass[DataSource](this)
    }
  }

  override def read(kryo: Kryo, input: Input, `type`: Class[DataSource]): DataSource = {
    val flag = input.readByte().toInt
    if (flag == 0) {
      kryo.readClassAndObject(input).asInstanceOf[JdbcSource]
    }
    else {
      null.asInstanceOf[DataSource]
    }
  }

  override def write(kryo: Kryo, output: Output, ds: DataSource): Unit = {
    ds match {
      case jdbc: JdbcSource ⇒
        output.writeByte(0.toByte)
        kryo.writeClassAndObject(output, jdbc)
    }
  }
}
