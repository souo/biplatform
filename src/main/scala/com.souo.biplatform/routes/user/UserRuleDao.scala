package com.souo.biplatform.routes.user

import com.souo.biplatform.common.sql.SqlDatabase
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author souo
 */
class UserRuleDao(protected val database: SqlDatabase)(implicit val ec: ExecutionContext) extends UserRuleSchema {

  import database._
  import database.driver.api._

  def isAdmin(login: String): Future[Boolean] = {
    val q = userRule.filter { r ⇒
      r.login === login && r.state === 1
    }.map {
      _.ruleId
    }
    val ruleId = db.run(q.result.headOption)

    ruleId.map(_.exists(_ == 0))
  }

}

trait UserRuleSchema {
  protected val database: SqlDatabase

  import database.driver.api._
  import database.jodaSupport._

  protected val userRule = TableQuery[UserRules]

  protected class UserRules(tag: Tag) extends Table[UserRule](tag, "bi_users_rule") {

    val id: Rep[Option[Int]] = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)
    val login = column[String]("login")
    val ruleId = column[Int]("rule_id")
    val state = column[Int]("state", O.Default(1))
    val createOn = column[DateTime]("create_on")

    def * = (id, login, ruleId, state, createOn) <> (UserRule.tupled, UserRule.unapply)
  }

}

case class UserRule(
  id:       Option[Int] = None,
  login:    String,
  rule_id:  Int,
  state:    Int,
  createOn: DateTime)

