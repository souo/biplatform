package com.souo.biplatform.admin

import slick.jdbc.JdbcBackend._

import cats.syntax.either._
import com.typesafe.scalalogging.StrictLogging

/**
 * Created by souo on 2016/12/13
 */
class DataSourceService(adminConfig: AdminConfig) extends StrictLogging {

  def withDB[T](f: slick.jdbc.JdbcBackend.Database ⇒ Throwable Either T): Throwable Either T = {
    val db = Either.catchNonFatal{
      Database.forConfig(adminConfig.mysqlDataSourceKey, adminConfig.rootConfig)
    }
    val result = db.flatMap { d ⇒
      f(d)
    }
    //close the db
    db.foreach { x ⇒
      logger.debug("close the db ...")
      x.close()
    }
    result
  }

  def listAllTables(): Either[Throwable, List[TableInfo]] = withDB { db ⇒
    Either.catchNonFatal{
      val metaData = db.source.createConnection().getMetaData
      val rs = metaData.getTables(null, null, "%", Array("TABLE"))
      var result = List.empty[TableInfo]
      while (rs != null && rs.next) {
        val tableName = rs.getString("TABLE_NAME")
        val comment = rs.getString("REMARKS")
        result ::= TableInfo(tableName, tableName, comment)
      }
      result
    }
  }

  def listAllColumns(tableId: String): Either[Throwable, List[ColumnInfo]] = withDB { db ⇒
    Either.catchNonFatal{
      val metaData = db.source.createConnection().getMetaData
      val rs = metaData.getColumns(null, null, tableId, "%")
      var result = List.empty[ColumnInfo]
      while (rs != null && rs.next) {
        val tableName = rs.getString("COLUMN_NAME")
        val comment = rs.getString("REMARKS")
        val dt = rs.getString("TYPE_NAME")
        result ::= ColumnInfo(tableName, tableName, comment, dt)
      }
      result
    }.flatMap { rs ⇒
      rs.isEmpty match {
        case true  ⇒ Left(NoSuchTableError(s"table $tableId not found"))
        case false ⇒ Right(rs)
      }
    }
  }
}

case class NoSuchTableError(msg: String) extends Throwable(msg)
case class TableInfo(id: String, name: String, comment: String)
case class ColumnInfo(id: String, name: String, comment: String, dataType: String)
