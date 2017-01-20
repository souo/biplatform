package com.souo.biplatform.serializer

import com.esotericsoftware.kryo.Kryo
import org.joda.time.DateTime

/**
 * Created by souo on 2017/1/1
 */
class KryoSerializerInit {

  def customize(kryo: Kryo): Unit = {
    kryo.register(classOf[DateTime], new JodaDateTimeSerializer)
    kryo.register(classOf[Some[_]], new SomeSerializer)
    kryo.setReferences(false)
  }

}
