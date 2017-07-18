package com.souo.biplatform.system.cluster

import com.souo.biplatform.system.UserNode
import akka.cluster.sharding.ShardRegion

/**
 * @author souo
 */
object ShardUsers {

  val shardName: String = "shardUsers"

  val numberOfShards = 100

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: UserNode.Command ⇒ (UserNode.name(cmd.login), cmd)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: UserNode.Command ⇒ (cmd.login.hashCode % numberOfShards).toString
  }

}

