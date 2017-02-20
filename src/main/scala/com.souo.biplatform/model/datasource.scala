package com.souo.biplatform.model

import java.io.File

import org.apache.commons.io.FileUtils

import scala.collection.JavaConversions._
import cats.syntax.either._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import slick.jdbc.JdbcBackend._

import scala.io.Source

/**
  * Created by souo on 2017/1/24
  */
sealed trait DataSource {
  val tpe: String
  val option: Map[String, AnyRef]

  def listAllTables(): Either[Throwable, List[TableInfo]]

  def listAllColumns(tableId: String): Either[Throwable, List[ColumnInfo]]
}

case class CsvSource(path: String, sep: String) extends DataSource {
  val dir = new File(path)
  require(dir.isDirectory, "必须指定一个正确的目录")
  override val tpe: String = "csv"
  override val option: Map[String, AnyRef] = Map(
    "path" → path,
    "seq" → sep
  )

  override def listAllTables(): Either[Throwable, List[TableInfo]] = {
    Either.catchNonFatal {
      val csvFiles = FileUtils.listFiles(dir, Array("csv"), true).toList

      csvFiles.map(file ⇒
        TableInfo(file.getName, file.getName, ""))
    }
  }

  override def listAllColumns(tableId: String): Either[Throwable, List[ColumnInfo]] = {
    Either.catchNonFatal {
      val file = new File(dir, tableId)
      //找到第一个非空的行
      val firstLine = Source.fromFile(file).getLines().toStream.find(_.nonEmpty).get

      firstLine.split(sep).map { name ⇒
        ColumnInfo(
          name,
          name,
          "",
          "string"
        )
      }.toList
    }
  }
}


object MySqlServer {

  val driver =  "com.mysql.cj.jdbc.Driver"

  def url(host: String, port: Int, database: String) = "jdbc:mysql://" + host + ":" + port + "/" + database

  def defaultConfigMap = Map[String, AnyRef](
    "numThreads" -> new Integer(5)
  )

}


case class MysqlSource(host: String,
                       port: Int,
                       db: String,
                       user: String,
                       pwd: String) extends DataSource with StrictLogging {
  override val tpe: String = "mysql"

  override val option: Map[String, AnyRef] = Map(
    "url" -> MySqlServer.url(host, port, db),
    "driver" -> MySqlServer.driver,
    "user" → user,
    "password" → pwd,
    "connectionPool" -> "disabled"
  ) ++ MySqlServer.defaultConfigMap

  def withDB[T](f: slick.jdbc.JdbcBackend.Database ⇒ Throwable Either T): Throwable Either T = {
    val db = Either.catchNonFatal {
      Database.forConfig("", ConfigFactory.parseMap(option))
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

  override def listAllTables(): Either[Throwable, List[TableInfo]] = withDB { db ⇒
    Either.catchNonFatal {
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
    Either.catchNonFatal {
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
        case true ⇒ Left(NoSuchTableError(s"table $tableId not found"))
        case false ⇒ Right(rs)
      }
    }
  }
}


case class NoSuchTableError(msg: String) extends Throwable(msg)

case class TableInfo(id: String, name: String, comment: String)

case class ColumnInfo(id: String, name: String, comment: String, dataType: String)

object DataSources {
  def csv(path: String, sep: String = ",") = CsvSource(path, sep)

  def mysql(host: String,
            port: Int,
            db: String, user: String, pwd: String) = {
    MysqlSource(host, port, db, user, pwd)
  }

}
