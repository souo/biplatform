package com.souo.biplatform.common.api

import akka.http.scaladsl.server.{AuthorizationFailedRejection, ExceptionHandler, RejectionHandler}
import com.typesafe.scalalogging.StrictLogging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import com.souo.biplatform.common.api.Response._
import io.circe.generic.auto._

import scala.util.control.NonFatal

/**
 * @author souo
 */
trait RoutesRequestWrapper extends CirceSupport with StrictLogging {

  private val exceptionHandler = ExceptionHandler {
    case NonFatal(e) ⇒
      logger.error(s"Exception during client request processing: ${e.getMessage}", e)
      _.complete(StatusCodes.InternalServerError, Error(message = "Internal server error", moreInfo = e.getMessage))
  }

  private val rejectionHandler = RejectionHandler.newBuilder().handle {
    case AuthorizationFailedRejection ⇒
      complete(Forbidden, error("The supplied authentication is not authorized to access this resource"))
  }.result().withFallback(RejectionHandler.default)

  private val logDuration = extractRequestContext.flatMap { ctx ⇒
    val start = System.currentTimeMillis()
    // handling rejections here so that we get proper status codes
    mapResponse { resp ⇒
      val d = System.currentTimeMillis() - start
      logger.info(s"[${resp.status.intValue()}] ${ctx.request.method.name} ${ctx.request.uri} took: ${d}ms")
      resp
    } & handleRejections(rejectionHandler)
  }

  val requestWrapper = logDuration &
    handleExceptions(exceptionHandler) &
    encodeResponse

}
