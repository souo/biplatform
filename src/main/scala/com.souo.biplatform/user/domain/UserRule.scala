package com.souo.biplatform.user.domain

import org.joda.time.DateTime

/**
 * Created by souo on 2016/12/12
 */
case class UserRule(
  id:       Option[Int] = None,
  login:    String,
  rule_id:  Int,
  state:    Int,
  createOn: DateTime
)
