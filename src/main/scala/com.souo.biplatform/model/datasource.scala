package com.souo.biplatform.model

import java.sql.{Connection, DriverManager, ResultSet}

import cats.syntax.either._
import com.mysql.cj.jdbc.{Driver ⇒ MysqlDriver}
import org.apache.kylin.jdbc.{Driver ⇒ KylinDriver}
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.model.DataType.DataType
import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.collection.mutable.ListBuffer

/**
 * @author souo
 */

trait DataSourceFunction {
  def check(): Result[Unit]
  def listAllTables(): Result[List[TableInfo]]
  def listAllColumns(tableId: String): Result[List[ColumnInfo]]

  def getDimValues(tableName: String, dimName: String): Result[List[String]]
}

@ApiModelProperty(hidden = true)
trait DataSource extends DataSourceFunction

object JdbcSourceType {
  val MYSQL = "mysql"
  val KYLIN = "kylin"
}

case class JdbcSource(
  `type`: String,
  host:   String,
  port:   Int,
  user:   String,
  pwd:    String,
  db:     String) extends DataSource with JdbcSourceFunctionImpl {

  val url = `type` match {
    case JdbcSourceType.MYSQL ⇒ s"jdbc:mysql://$host:$port/$db"
    case JdbcSourceType.KYLIN ⇒ s"jdbc:kylin://$host:$port/$db"
  }

  val driver = `type` match {
    case JdbcSourceType.MYSQL ⇒ classOf[MysqlDriver].getCanonicalName
    case JdbcSourceType.KYLIN ⇒ classOf[KylinDriver].getCanonicalName
  }

}

trait JdbcSourceFunctionImpl extends DataSourceFunction with StrictLogging {
  this: JdbcSource ⇒

  def getConnection: Result[Connection] = {
    Either.catchNonFatal[Connection] {
      logger.info(s"load driver:$driver")
      Class.forName(driver)
      DriverManager.getConnection(url, user, pwd)
    }
  }

  override def check(): Result[Unit] = getConnection.map(_.close()).leftMap { t ⇒
    InValidConnectParam(
      "无法连接指定的数据源，请检查配置参数")
  }
  def withDb[T](f: (Connection) ⇒ Result[T]): Result[T] = {
    logger.info("open jdbc connection")
    val connection = getConnection
    try {
      connection.leftMap{ t ⇒
        t.printStackTrace()
      }
      connection.flatMap(f)
    }
    finally {
      connection.foreach { c ⇒
        logger.info("closing jdbc connection...")
        c.close()
      }
    }
  }

  override def listAllTables(): Result[List[TableInfo]] = {
    withDb { connection ⇒
      Either.catchNonFatal {
        val metaData = connection.getMetaData
        val rs = metaData.getTables(null, null, "%", Array("TABLE"))
        var result = List.empty[TableInfo]
        while (rs != null && rs.next) {
          val tableName = rs.getString("TABLE_NAME")
          val comment = rs.getString("REMARKS")
          result ::= TableInfo(tableName, tableName, comment)
        }
        result
      }.leftMap{ throwable ⇒
        throwable.printStackTrace()
        throwable
      }
    }
  }

  override def listAllColumns(tableId: String): Result[List[ColumnInfo]] = {
    withDb { connection ⇒
      Either.catchNonFatal {
        val metaData = connection.getMetaData
        val rs = metaData.getColumns(null, null, tableId, "%")
        var result = List.empty[ColumnInfo]
        while (rs != null && rs.next) {
          val tableName = rs.getString("COLUMN_NAME")
          val comment = rs.getString("REMARKS")
          val dt = DataTypes(rs.getString("TYPE_NAME"))
          result ::= ColumnInfo(tableName, tableName, comment, dt)
        }
        result
      }.leftMap{ throwable ⇒
        throwable.printStackTrace()
        throwable
      }
    }
  }

  override def getDimValues(tableName: String, dimName: String): Result[List[String]] = {
    withDb{ conn ⇒
      Either.catchNonFatal{
        val sql = s"SELECT DISTINCT $dimName FROM $tableName"
        val stmt = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
        stmt.setFetchSize(100)
        val rs = stmt.executeQuery()
        val res = new ListBuffer[String]()
        while (rs.next()) {
          res += rs.getString(1)
        }
        rs.close()
        stmt.close()
        res.toList
      }
    }
  }
}

case class TableInfo(id: String, name: String, comment: String)

@ApiModel
case class ColumnInfo(
  id:      String,
  name:    String,
  comment: String,
  @ApiModelProperty(
    dataType        = "string",
    allowableValues = "STRING,DATE,NUMERIC") dataType: DataType)

object DataSource {
  def jdbc(
    `type`: String,
    host:   String,
    port:   Int,
    db:     String,
    user:   String,
    pwd:    String): JdbcSource = {
    JdbcSource(`type`, host, port, db, user, pwd)
  }
}

case class InValidConnectParam(msg: String) extends RuntimeException
