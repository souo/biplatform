package com.souo.biplatform.system.cluster

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import com.souo.biplatform.system.UserNode

/**
 * Created by souo on 2017/1/3
 */
trait UserNodeShard {
  val system: ActorSystem

  ClusterSharding(system).start(
    typeName        = ShardUsers.shardName,
    entityProps     = UserNode.props,
    settings        = ClusterShardingSettings(system).withRole(Roles.designer),
    extractEntityId = ShardUsers.extractEntityId,
    extractShardId  = ShardUsers.extractShardId
  )

  def shardUser: ActorRef = {
    ClusterSharding(system).shardRegion(ShardUsers.shardName)
  }
}
