package com.souo.biplatform.user.application

import com.souo.biplatform.user.User

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by souo on 2016/12/12
 */
class UserService(userRuleDao: UserRuleDao)(implicit ec: ExecutionContext) {

  def isAdminUser(user: User): Future[Boolean] = {
    userRuleDao.isAdmin(user.login)
  }

}
