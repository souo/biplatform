package com.souo.biplatform.system.cluster

import akka.actor.{ActorSystem, PoisonPill}
import akka.cluster.singleton._
import com.souo.biplatform.system.{CubeNode, DataSourceNode}

/**
 * Created by souo on 2017/1/24
 */
trait DSNodeSingleton {
  val system: ActorSystem

  val singletonManager = system.actorOf(
    ClusterSingletonManager.props(
      DataSourceNode.props,
      PoisonPill,
      ClusterSingletonManagerSettings(system)
        .withRole(Roles.designer)
        .withSingletonName(DataSourceNode.name)
    )
  )

  val dsNode = system.actorOf(
    ClusterSingletonProxy.props(
      singletonManager.path.child(DataSourceNode.name)
      .toStringWithoutAddress,
      ClusterSingletonProxySettings(system)
        .withRole(Roles.designer)
        .withSingletonName("ds-proxy")
    )
  )
}
