package com.souo.biplatform

import java.util.UUID

import com.softwaremill.session.{MultiValueSessionSerializer, SessionSerializer}

import scala.util.Try

/**
 * @author souo
 */
package object user {

  case class User(login: String)

  case class Session(user: User)

  object Session {
    implicit val serializer: SessionSerializer[Session, String] = new MultiValueSessionSerializer[Session](
      (t: Session) ⇒ Map("login" → t.user.login),
      (m) ⇒ Try {
        Session(User(m("login")))
      }
    )
  }

}
