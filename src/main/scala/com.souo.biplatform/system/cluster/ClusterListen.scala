package com.souo.biplatform.system.cluster

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

/**
 * @author souo
 */
class ClusterListen extends Actor with ActorLogging {
  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[ClusterDomainEvent])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive = {
    case m: MemberUp          ⇒ log.info(s"Member ${m.member.address} is up with roles ${m.member.roles}")
    case m: UnreachableMember ⇒ log.info(s"Member ${m.member.address} is unreachable  ${m.member.roles}")
    case m: ReachableMember   ⇒ log.info(s"Member ${m.member.address} is reachable  ${m.member.roles}")
    case m: MemberRemoved     ⇒ log.info(s"Member ${m.member.address} is removed  ${m.member.roles}")
    case m: MemberExited      ⇒ log.info(s"Member ${m.member.address} is exited  ${m.member.roles}")
  }
}
