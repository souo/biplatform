package com.souo.biplatform.model

import com.softwaremill.session.{MultiValueSessionSerializer, SessionSerializer}

import scala.util.Try

/**
 * @author souo
 */
case class Session(user: UserWithRole)

object Session {
  implicit val serializer: SessionSerializer[Session, String] = new MultiValueSessionSerializer[Session](
    (t: Session) ⇒ Map("login" → t.user.login, "isAdmin" → t.user.isAdmin.toString),
    (m) ⇒ Try {
      Session(UserWithRole(m("login"), m("isAdmin").toBoolean))
    }
  )
}