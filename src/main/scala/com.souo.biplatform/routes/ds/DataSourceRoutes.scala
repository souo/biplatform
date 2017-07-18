package com.souo.biplatform.routes.ds

import javax.ws.rs.Path

import akka.actor.ActorRef
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.common.api.Response._
import com.souo.biplatform.common.api._
import com.souo.biplatform.model._
import com.souo.biplatform.routes.ds.RequestParams.JdbcConfig
import com.souo.biplatform.system.DataSourceNode
import com.souo.biplatform.system.DataSourceNode._
import com.souo.biplatform.routes.user.UserService
import io.circe.Encoder
import io.circe.generic.auto._
import io.swagger.annotations._

/**
 * @author souo
 */
@Api(tags = Array("DataSource"), produces = "application/json")
@Path("/api/datasource")
trait DataSourceRoutes extends RoutesSupport with StrictLogging with SessionSupport with DefaultTimeOut {

  val userService: UserService
  val dsNode: ActorRef

  implicit val encoderItem: Encoder[Item] = {
    Encoder.forProduct5(
      "dsId",
      "name",
      "createBy",
      "modifyBy",
      "lastModifyTime"
    ){ item ⇒
        (item.dsId, item.name, item.createBy, item.modifyBy, item.lastModifyTime)
      }
  }

  val dsRoutes = pathPrefix("datasource") {
    listAllDS ~
      addDataSource ~
      listAllTables ~
      listAllColumns ~
      getDs ~
      updateDs ~
      removeDs
  }

  @Path("")
  @ApiOperation(
    value      = "获取数据源列表",
    httpMethod = "GET"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "query",
        paramType = "query",
        dataType  = "string",
        required  = false
      )
    )
  )
  @ApiResponses(
    Array(
      new ApiResponse(
        code              = 200,
        message           = "成功",
        response          = classOf[DataSourceNode.Item],
        responseContainer = "List"
      )
    )
  )
  def listAllDS = { //获取定义的所有 datasource
    (get & pathEnd) {
      parameter("query" ?) { query ⇒
        userFromSession { user ⇒
          if (user.isAdmin) {
            val cmd = DataSourceNode.ListAllDS(query)
            onSuccess((dsNode ? cmd).mapTo[Result[List[DataSourceNode.Item]]]) {
              case Right(items) ⇒
                complete(ok(items))
              case Left(t) ⇒
                complete(error(t))
            }
          }
          else {
            logger.warn(s"login user ${user.login} is not allow access this resource")
            reject(AuthorizationFailedRejection)
          }
        }
      }
    }
  }

  @Path("/{dsId}/tables")
  @ApiOperation(
    value      = "获取数据源下所有表",
    httpMethod = "GET"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "dsId",
        paramType = "path",
        dataType  = "java.util.UUID",
        required  = true
      )
    )
  )
  @ApiResponses(
    Array(
      new ApiResponse(
        code              = 200,
        message           = "OK",
        response          = classOf[TableInfo],
        responseContainer = "List"
      )
    )
  )
  def listAllTables = {
    (get & path(JavaUUID / "tables")) { dsId ⇒
      userFromSession { user ⇒
        if (user.isAdmin) {
          val cmd = DataSourceNode.ListAllTables(dsId)
          onSuccess((dsNode ? cmd).mapTo[Result[List[TableInfo]]]) {
            case Left(t) ⇒
              complete(error(t))
            case Right(r) ⇒
              complete(ok(r))
          }
        }
        else {
          logger.warn(s"login user ${user.login} is not allow access this resource")
          reject(AuthorizationFailedRejection)
        }
      }
    }
  }

  @Path("/{dsId}/tables/{tableId}/fields")
  @ApiOperation(
    value      = "获取数据源下某个表的所有字段",
    httpMethod = "GET"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "dsId",
        paramType = "path",
        dataType  = "java.util.UUID",
        required  = true
      ),
      new ApiImplicitParam(
        name      = "tableId",
        paramType = "path",
        dataType  = "string",
        required  = true
      )
    )
  )
  @ApiResponses(
    Array(
      new ApiResponse(
        code              = 200,
        message           = "OK",
        response          = classOf[ColumnInfo],
        responseContainer = "List"
      )
    )
  )
  def listAllColumns = {
    (get & path(JavaUUID / "tables" / Segment / "fields")) { (dsId, tid) ⇒
      userFromSession { user ⇒
        if (user.isAdmin) {
          val cmd = DataSourceNode.ListAllColumns(dsId, tid)
          onSuccess((dsNode ? cmd).mapTo[Result[List[ColumnInfo]]]) {
            case Left(t) ⇒
              complete(error(t))
            case Right(r) ⇒
              complete(ok(r))
          }
        }
        else {
          logger.warn(s"login user ${user.login} is not allow access this resource")
          reject(AuthorizationFailedRejection)
        }
      }
    }
  }

  @Path("")
  @ApiOperation(
    value      = "创建一个jdbc数据源",
    httpMethod = "POST",
    consumes   = "application/json"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        value     = "参数",
        paramType = "body",
        dataType  = "com.souo.biplatform.routes.ds.RequestParams$JdbcConfig",
        required  = true
      )
    )
  )
  @ApiResponses(
    Array(
      new ApiResponse(
        code    = 200,
        message = "成功"
      )
    )
  )
  def addDataSource = {
    (post & pathEnd) {
      userFromSession { user ⇒
        if (user.isAdmin) {
          entity(as[RequestParams.JdbcConfig]) { config ⇒
            import config.{user ⇒ dbUser, _}
            val ds = JdbcSource(`type`, host, port, db, dbUser, pwd)
            val cmd = DataSourceNode.Add(user.login, name, ds)
            onSuccess((dsNode ? cmd).mapTo[Result[Unit]]) {
              case Right(_) ⇒
                complete(ok())
              case Left(InValidName(msg)) ⇒
                complete(Error(
                  status   = 5001,
                  message  = "invalid name",
                  moreInfo = msg
                ))
              case Left(InValidConnectParam(msg)) ⇒
                complete(Error(
                  status   = 5002,
                  message  = "invalid connection",
                  moreInfo = msg
                ))

              case Left(t) ⇒
                complete(error(t))
            }
          }
        }
        else {
          logger.warn(s"login user ${user.login} is not allow access this resource")
          reject(AuthorizationFailedRejection)
        }
      }
    }
  }

  @Path("/{dsId}")
  @ApiOperation(
    value      = "更新一个jdbc数据源",
    httpMethod = "PUT",
    consumes   = "application/json"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "dsId",
        paramType = "path",
        dataType  = "java.util.UUID",
        required  = true
      ),
      new ApiImplicitParam(
        value     = "参数",
        paramType = "body",
        dataType  = "com.souo.biplatform.routes.ds.RequestParams$JdbcConfig",
        required  = true
      )
    )
  )
  @ApiResponses(
    Array(
      new ApiResponse(
        code    = 200,
        message = "成功"
      )
    )
  )
  def updateDs = {
    put{
      path(JavaUUID){ id ⇒
        userFromSession { user ⇒
          if (user.isAdmin) {
            entity(as[RequestParams.JdbcConfig]){ config ⇒
              import config.{user ⇒ dbUser, _}
              val ds = JdbcSource(`type`, host, port, db, dbUser, pwd)
              val cmd = DataSourceNode.Update(user.login, name, id, ds)
              onSuccess((dsNode ? cmd).mapTo[Result[Unit]]) {
                case Right(_) ⇒
                  complete(ok())
                case Left(InValidName(msg)) ⇒
                  complete(Error(
                    status   = 5001,
                    message  = "invalid name",
                    moreInfo = msg
                  ))
                case Left(InValidConnectParam(msg)) ⇒
                  complete(Error(
                    status   = 5002,
                    message  = "invalid connection",
                    moreInfo = msg
                  ))

                case Left(t) ⇒
                  complete(error(t))
              }
            }
          }
          else {
            logger.warn(s"login user ${user.login} is not allow access this resource")
            reject(AuthorizationFailedRejection)
          }
        }
      }
    }
  }

  @Path("/{dsId}")
  @ApiOperation(
    value      = "获取一个jdbc数据源的配置信息",
    httpMethod = "GET"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "dsId",
        paramType = "path",
        dataType  = "java.util.UUID",
        required  = true
      )
    )
  )
  @ApiResponses(
    Array(
      new ApiResponse(
        code     = 200,
        message  = "成功",
        response = classOf[Item]
      )
    )
  )
  def getDs = {
    get {
      path(JavaUUID) { id ⇒
        userFromSession { user ⇒
          if (user.isAdmin) {
            val cmd = DataSourceNode.Get(id)
            onSuccess((dsNode ? cmd).mapTo[Result[Item]]) {
              case Left(t) ⇒
                complete(error(t))
              case Right(item) ⇒
                item.dataSource match {
                  case ds: JdbcSource ⇒
                    complete(
                      ok[JdbcConfig](JdbcConfig(
                        ds.`type`,
                        item.name,
                        host = ds.host,
                        port = ds.port,
                        db   = ds.db,
                        user = ds.user,
                        pwd  = ds.pwd
                      ))
                    )
                  case _ ⇒
                    failWith(new RuntimeException("not support"))
                }
            }
          }
          else {
            logger.warn(s"login user ${user.login} is not allow access this resource")
            reject(AuthorizationFailedRejection)
          }
        }
      }
    }
  }

  @Path("/{dsId}")
  @ApiOperation(
    value      = "删除一个jdbc数据源",
    httpMethod = "DELETE"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "dsId",
        paramType = "path",
        dataType  = "java.util.UUID",
        required  = true
      )
    )
  )
  @ApiResponses(
    Array(
      new ApiResponse(
        code    = 200,
        message = "成功"
      )
    )
  )
  def removeDs = {
    (delete & path(JavaUUID)) { id ⇒
      userFromSession { user ⇒
        if (user.isAdmin) {
          val cmd = DataSourceNode.Remove(id)
          onSuccess((dsNode ? cmd).mapTo[Result[Unit]]) {
            case Left(t) ⇒
              complete(error(t))
            case Right(_) ⇒
              complete(ok())
          }
        }
        else {
          logger.warn(s"login user ${user.login} is not allow access this resource")
          reject(AuthorizationFailedRejection)
        }
      }
    }
  }

}
