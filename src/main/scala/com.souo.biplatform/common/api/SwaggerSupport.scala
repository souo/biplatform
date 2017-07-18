package com.souo.biplatform.common.api

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging

import scala.reflect.runtime.universe.Type
import io.swagger.jaxrs.Reader
import io.swagger.jaxrs.config.ReaderConfig
import io.swagger.models.{ExternalDocs, Scheme, Swagger}
import io.swagger.models.auth.SecuritySchemeDefinition
import io.swagger.util.{Json, Yaml}
import org.apache.commons.lang3.StringUtils
import com.souo.biplatform.model.Swagger._

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/**
 * @author souo
 */
object SwaggerSupport {

  val readerConfig = new ReaderConfig {
    def getIgnoredRoutes: java.util.Collection[String] = List[String]().asJavaCollection
    def isScanAllResources: Boolean = false
  }

  def toJavaTypeSet(apiTypes: Seq[Type]): Set[Class[_]] = {
    apiTypes.map(t ⇒ getClassForType(t)).toSet
  }

  private lazy val mirror = scala.reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)

  def getClassForType(t: Type): Class[_] = {
    mirror.runtimeClass(t.typeSymbol.asClass)
  }

  def removeInitialSlashIfNecessary(path: String): String =
    if (path.startsWith("/")) {
      removeInitialSlashIfNecessary(path.substring(1))
    }
    else {
      path
    }
}

trait SwaggerSupport extends StrictLogging {
  implicit val actorSystem: ActorSystem
  implicit val materializer: ActorMaterializer

  import SwaggerSupport._

  val apiTypes: Seq[Type]
  val host: String = ""
  val basePath: String = "/"
  val apiDocsPath: String = "api-docs"
  val info: Info = Info()
  val scheme: Scheme = Scheme.HTTP
  val securitySchemeDefinitions: Map[String, SecuritySchemeDefinition] = Map()
  val externalDocs: Option[ExternalDocs] = None

  def swaggerConfig: Swagger = {
    val modifiedPath = prependSlashIfNecessary(basePath)
    val swagger = new Swagger().basePath(modifiedPath).info(info).scheme(scheme)
    if (StringUtils.isNotBlank(host)) swagger.host(host)
    swagger.setSecurityDefinitions(securitySchemeDefinitions.asJava)
    externalDocs match {
      case Some(ed) ⇒ swagger.externalDocs(ed)
      case None     ⇒ swagger
    }
  }

  def reader = new Reader(swaggerConfig, readerConfig)
  def prependSlashIfNecessary(path: String): String = if (path.startsWith("/")) path else s"/$path"

  def generateSwaggerJson: String = {
    try {
      val swagger: Swagger = reader.read(toJavaTypeSet(apiTypes).asJava)
      Json.pretty().writeValueAsString(swagger)
    }
    catch {
      case NonFatal(t) ⇒ {
        logger.error("Issue with creating swagger.json", t)
        throw t
      }
    }
  }

  def generateSwaggerYaml: String = {
    try {
      val swagger: Swagger = reader.read(toJavaTypeSet(apiTypes).asJava)
      Yaml.pretty().writeValueAsString(swagger)
    }
    catch {
      case NonFatal(t) ⇒ {
        logger.error("Issue with creating swagger.yaml", t)
        throw t
      }
    }
  }

  lazy val apiDocsBase = PathMatchers.separateOnSlashes(removeInitialSlashIfNecessary(apiDocsPath))

  lazy val routes: Route =
    path(apiDocsBase / "swagger.json") {
      get {
        complete(HttpEntity(MediaTypes.`application/json`, generateSwaggerJson))
      }
    } ~
      path(apiDocsBase / "swagger.yaml") {
        get {
          complete(HttpEntity(CustomMediaTypes.`text/vnd.yaml`, generateSwaggerYaml))
        }
      }
}
