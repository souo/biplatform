package com.souo.biplatform.serializer

import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.{Kryo, Serializer}

/**
 * Created by souo on 2017/1/3
 */
class SomeSerializer extends Serializer[Some[_]] {
  override def write(kryo: Kryo, output: Output, t: Some[_]): Unit = {
    kryo.writeClassAndObject(output, t.get)
  }

  override def read(kryo: Kryo, input: Input, aClass: Class[Some[_]]): Some[_] = {
    Some(kryo.readClassAndObject(input))
  }
}
