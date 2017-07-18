package com.souo.biplatform.model

/**
 * @author souo
 */
case class DBTime(time: Long)

case class BiUser(
  userId:        Option[Int],
  userName:      Option[String],
  loginName:     Option[String],
  loginPassword: Option[String],
  roleId:        Option[Int],
  appId:         Option[Int],
  status:        Boolean
)
