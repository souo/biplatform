package com.souo.biplatform.test

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.softwaremill.session.{SessionConfig, SessionManager}
import com.souo.biplatform.common.api.CirceSupport
import com.souo.biplatform.user.Session
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.Matchers

/**
 * Created by souo on 2016/12/19
 */
trait BaseRoutesSpec extends FlatSpecWithDb
    with ScalatestRouteTest
    with Matchers
    with CirceSupport {
  spec ⇒

  override def testConfig: Config = {
    ConfigFactory.load(ConfigFactory.parseString(
      """
        |akka {
        |  // Logging.
        |  loglevel = INFO
        |  actor.debug.receive = on
        |  actor.debug.autoreceive = on
        |  actor.debug.event-stream = on
        |  actor.debug.lifecycle = on
        |  log-dead-letters = off
        |
        |  // Persistence.
        |  persistence {
        |    journal.plugin = "akka.persistence.journal.inmem"
        |    snapshot-store.plugin = "akka.persistence.snapshot-store.local"
        |    snapshot-store.local.dir = "target/snapshots"
        |  }
        |
        |  jvm-exit-on-fatal-error = off
        |  actor.default-mailbox.mailbox-type = "akka.dispatch.UnboundedControlAwareMailbox"
        |}
        |
        |
        |akka.http.session {
        |  server-secret = "sf9s0f9ds0f03rsjrjfsdjfldsjfi32lkkjkjjsfljlfjsldfjsdu9032094hdsf-3k;k;s"
        |  server-secret = ${?SERVER_SECRET}
        |}
        |
        |
        |akka-http-routes-dispatcher {
        |  # these are the default dispatcher settings
        |  type = "Dispatcher"
        |  executor = "fork-join-executor"
        |
        |  fork-join-executor {
        |    parallelism-min = 8
        |    parallelism-factor = 3.0
        |    parallelism-max = 64
        |  }
        |  throughput = 5
        |}
        |
        |dao-dispatcher {
        |  # these are the default dispatcher settings
        |  type = "Dispatcher"
        |  executor = "fork-join-executor"
        |
        |  fork-join-executor {
        |    parallelism-min = 8
        |    parallelism-factor = 3.0
        |    parallelism-max = 64
        |  }
        |  throughput = 5
        |}
        |
        |
        |service-dispatcher {
        |  # these are the default dispatcher settings
        |  type = "Dispatcher"
        |  executor = "fork-join-executor"
        |
        |  fork-join-executor {
        |    parallelism-min = 8
        |    parallelism-factor = 3.0
        |    parallelism-max = 64
        |  }
        |
        |  throughput = 5
        |}
      """.stripMargin
    ))
  }

  lazy val sessionConfig = SessionConfig.fromConfig(testConfig)
    .copy(sessionEncryptData = true)

  trait TestRoutesSupport {
    lazy val sessionConfig = spec.sessionConfig

    implicit def materializer = spec.materializer

    implicit def ec = spec.executor

    implicit def sessionManager = new SessionManager[Session](sessionConfig)
  }

}
