package com.souo.biplatform.user.api

import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1}
import akka.http.scaladsl.server.Directives.{provide, reject}
import com.softwaremill.session.SessionDirectives.session
import com.softwaremill.session.{RefreshTokenStorage, SessionManager}
import com.softwaremill.session.SessionOptions.{oneOff, refreshable, usingCookies}
import com.souo.biplatform.SystemEnv
import com.souo.biplatform.user.{Session, User}

import scala.concurrent.ExecutionContext

/**
 * @author souo
 */
trait SessionSupport {

  implicit def sessionManager: SessionManager[Session]

  implicit def ec: ExecutionContext

  def userFromSession: Directive1[User] = {
    session(oneOff, usingCookies).flatMap {
      _.toOption match {
        case None ⇒
          if (SystemEnv.isTest) {
            provide(User("admin"))
          }
          else {
            reject(AuthorizationFailedRejection)
          }
        case Some(s) ⇒
          provide(s.user)
      }
    }
  }
}
