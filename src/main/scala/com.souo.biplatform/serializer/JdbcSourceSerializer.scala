package com.souo.biplatform.serializer

import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.{Kryo, Serializer}
import com.twitter.chill.{IKryoRegistrar, toRich}
import com.souo.biplatform.model.JdbcSource

/**
 * @author souo
 */
class JdbcSourceSerializer extends Serializer[JdbcSource] with IKryoRegistrar {

  override def apply(kryo: Kryo): Unit = {
    if (!kryo.alreadyRegistered(classOf[JdbcSource])) {
      kryo.forClass[JdbcSource](this)
      kryo.forSubclass[JdbcSource](this)
    }
  }

  override def read(kryo: Kryo, input: Input, `type`: Class[JdbcSource]): JdbcSource = {

    val tpe = input.readString()
    val host = input.readString()
    val port = input.readInt()
    val user = input.readString()
    val pwd = input.readString()
    val db = input.readString()
    JdbcSource(tpe, host, port, user, pwd, db)
  }

  override def write(kryo: Kryo, output: Output, jdbc: JdbcSource): Unit = {
    output.writeString(jdbc.`type`)
    output.writeString(jdbc.host)
    output.writeInt(jdbc.port)
    output.writeString(jdbc.user)
    output.writeString(jdbc.pwd)
    output.writeString(jdbc.db)
  }
}
