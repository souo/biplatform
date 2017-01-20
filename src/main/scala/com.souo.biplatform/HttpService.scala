package com.souo.biplatform

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.softwaremill.session.{SessionConfig, SessionManager}
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.admin.AdminConfig
import com.souo.biplatform.common.sql.DatabaseConfig
import com.souo.biplatform.queryrouter.mysql.{MysqlEngine, MysqlStorage, MysqlStorageConfig, MysqlStorageNode}
import com.souo.biplatform.user.Session

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scala.language.reflectiveCalls

/**
 * Created by souo on 2017/1/3
 */
trait HttpService extends StrictLogging {

  def startService(cubeSingleton: ActorRef, userShard: ActorRef)(implicit actorSystem: ActorSystem): Unit = {
    implicit val ec = actorSystem.dispatcher
    implicit val materializer = ActorMaterializer()
    val _config = actorSystem.settings.config

    val modules = new DependencyWiring with Routes {
      override def system: ActorSystem = actorSystem

      override val config: ServerConfig with DatabaseConfig with AdminConfig with MysqlStorageConfig = {
        new ServerConfig with DatabaseConfig with AdminConfig with MysqlStorageConfig {
          override def rootConfig: Config = _config
        }
      }
      override val users: ActorRef = userShard
      override val cubeNode: ActorRef = cubeSingleton
      lazy val sessionConfig = SessionConfig.fromConfig(config.rootConfig).copy(sessionEncryptData = true)
      implicit lazy val sessionManager: SessionManager[Session] = new SessionManager[Session](sessionConfig)
      override implicit def ec: ExecutionContext = system.dispatchers.lookup("akka-http-routes-dispatcher")
      val mysqlStorage = new MysqlStorage(config)

      val mysqlEngine = new MysqlEngine(mysqlStorage)
      val mysqlStorageActor = system.actorOf(MysqlStorageNode.props(mysqlEngine), MysqlStorageNode.name)
    }

    logger.info(s"Server secret: ${modules.sessionConfig.serverSecret.take(3)} ...")
    modules.sqlDatabase.updateSchema()

    val host = modules.config.serverHost
    val port = modules.config.serverPort

    val startFuture = {
      Http().bindAndHandle(modules.routes, host, port)
    }

    startFuture.onComplete {
      case Success(b) ⇒
        logger.info(s"Server started on $host:$port")
        sys.addShutdownHook {
          b.unbind()
          actorSystem.terminate()
          logger.info("Server stopped")
        }
      case Failure(e) ⇒
        logger.error(s"Cannot start server on $host:$port", e)
        sys.addShutdownHook {
          actorSystem.terminate()
          logger.info("Server stopped")
        }
    }
  }

}
