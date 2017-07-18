package com.souo.biplatform.serializer

import akka.actor.ExtendedActorSystem
import com.twitter.chill.KryoInstantiator
import com.twitter.chill.akka.AkkaSerializer

/**
 * @author souo
 */
class MyKryoSerializer(system: ExtendedActorSystem) extends AkkaSerializer(system) {

  override def kryoInstantiator: KryoInstantiator = {
    super.kryoInstantiator
      .withRegistrar(new DataSourceSerializer)
      .withRegistrar(new JdbcSourceSerializer)
      .withRegistrar(new JodaDateTimeSerializer)
  }
}
