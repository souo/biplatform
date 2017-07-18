package com.souo.biplatform

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.softwaremill.session.{SessionConfig, SessionManager}
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.common.sql.DatabaseConfig
import com.souo.biplatform.model.Session

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scala.language.reflectiveCalls

/**
 * @author souo
 */
trait HttpService extends StrictLogging { SELF ⇒

  def startService(cube: ActorRef, _users: ActorRef, dataSource: ActorRef)(implicit _actorSystem: ActorSystem): Unit = {
    implicit val ec = _actorSystem.dispatcher
    implicit val _materializer = ActorMaterializer()
    val _config = _actorSystem.settings.config

    val modules = new DependencyWiring with Routes {
      override def system: ActorSystem = _actorSystem
      override val config: ServerConfig with DatabaseConfig = {
        new ServerConfig with DatabaseConfig {
          override def rootConfig: Config = _config
        }
      }

      override val users: ActorRef = _users
      override val cubeNode: ActorRef = cube
      override val dsNode: ActorRef = dataSource

      lazy val sessionConfig = SessionConfig.fromConfig(config.rootConfig).copy(sessionEncryptData = true)
      implicit lazy val sessionManager: SessionManager[Session] = new SessionManager[Session](sessionConfig)
      override implicit def ec: ExecutionContext = system.dispatchers.lookup("akka-http-routes-dispatcher")
    }

    logger.info(s"Server secret: ${modules.sessionConfig.serverSecret.take(3)} ...")
    modules.sqlDatabase.updateSchema()

    val host = modules.config.serverHost
    val port = modules.config.serverPort

    lazy val doc = new SwaggerRoutes {
      override implicit val materializer: ActorMaterializer = _materializer
      override implicit val actorSystem: ActorSystem = _actorSystem
    }.swagger

    val apiAndDoc = doc ~ modules.api
    val startFuture = {
      Http().bindAndHandle(apiAndDoc, host, port)
    }

    startFuture.onComplete {
      case Success(b) ⇒
        logger.info(s"Server started on $host:$port")
        sys.addShutdownHook {
          b.unbind()
          _actorSystem.terminate()
          logger.info("Server stopped")
        }
      case Failure(e) ⇒
        logger.error(s"Cannot start server on $host:$port", e)
        sys.addShutdownHook {
          _actorSystem.terminate()
          logger.info("Server stopped")
        }
    }
  }

}
