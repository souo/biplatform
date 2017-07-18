package com.souo.biplatform.system.cluster

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.HttpService
import com.souo.biplatform.common.util.IntParam

import scala.concurrent.duration._

/**
 * @author souo
 */
object ClusterMain extends HttpService
    with StrictLogging {
  var host = "0.0.0.0"
  var port = 0
  var http_port: Option[Int] = None

  def main(args: Array[String]): Unit = {

    parse(args.toList)
    val httpEnable = http_port.isDefined

    val config = ConfigFactory.parseString(
      s"""
         |akka {
         |  actor.provider = cluster
         |  remote {
         |    enabled-transports = [akka.remote.netty.tcp]
         |    netty.tcp {
         |      hostname = $host
         |      port = $port
         |    }
         |  }
         |cluster {
         |    auto-down-unreachable-after = 20s
         |    auto-down-unreachable-after = $${? AUTO_DOWN}
         |    seed-nodes = [$${SEED_NODE}]
         |  }
         |}
         |designer{
         |    http.enable=$httpEnable
         |    http.server{
         |       host=$host
         |       port=$http_port
         |    }
         |}
       """.stripMargin
    ).withFallback(ConfigFactory.load("cluster"))

    implicit val designerSystem = ActorSystem("designer", config)

    val cubeNode = new CubeNodeSingleton {
      override val system: ActorSystem = designerSystem
    }.cubeNode

    val shardUsers = new UserNodeShard {
      override val system: ActorSystem = designerSystem
      override val queryRouteNode: ActorRef = null
    }.shardUser

    designerSystem.actorOf(Props(classOf[ClusterListen]), "clusterListen")

    Cluster(designerSystem).registerOnMemberUp {
      logger.info("All cluster nodes are up!")
      //      startService(cubeNode, shardUsers)
    }

    Cluster(designerSystem).registerOnMemberRemoved {
      // exit JVM when ActorSystem has been terminated
      designerSystem.registerOnTermination(System.exit(-1))
      // in case ActorSystem shutdown takes longer than 10 seconds,
      // exit the JVM forcefully anyway
      designerSystem.scheduler.scheduleOnce(10.seconds)(System.exit(-1))(designerSystem.dispatcher)
      // shut down ActorSystem
      designerSystem.terminate()
    }
  }

  private def parse(args: List[String]): Unit = args match {
    case ("--ip" | "-i") :: value :: tail ⇒
      host = value
      parse(tail)

    case ("--host" | "-h") :: value :: tail ⇒
      host = value
      parse(tail)

    case ("--port" | "-p") :: IntParam(value) :: tail ⇒
      port = value
      parse(tail)

    case "--http-port" :: IntParam(value) :: tail ⇒
      http_port = Some(value)
      parse(tail)

    case ("--help") :: tail ⇒
      printUsageAndExit(0)

    case Nil ⇒ //
    case _ ⇒
      printUsageAndExit(1)
  }

  private def printUsageAndExit(exitCode: Int) {
    System.err.println(
      "Usage: ClusterMain [options]\n" +
        "\n" +
        "Options:\n" +
        "  -i HOST, --ip HOST     Hostname to listen on (deprecated, please use --host or -h) \n" +
        "  -h HOST, --host HOST   Hostname to listen on\n" +
        "  -p PORT, --port PORT   Port to listen on (default: 0)\n" +
        "  --http-port PORT       Port for http Server，note:" +
        " If you set this parameter, an http service use this port will start, otherwise it will not start )\n"
    )
    System.exit(exitCode)
  }
}
