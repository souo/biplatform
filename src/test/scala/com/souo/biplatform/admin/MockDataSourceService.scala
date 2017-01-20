package com.souo.biplatform.admin

import cats.syntax.either._
import com.typesafe.config.{Config, ConfigFactory}

/**
 * Created by souo on 2016/12/20
 */
class MockDataSourceService(adminConfig: AdminConfig) extends DataSourceService(adminConfig) {

  def this() = this(new AdminConfig {
    override def rootConfig: Config = ConfigFactory.empty()
  })

  override def withDB[T](f: (slick.jdbc.JdbcBackend.DatabaseDef) ⇒ Either[Throwable, T]): Either[Throwable, T] = {
    Either.left(new UnsupportedOperationException)
  }

  override def listAllTables(): Either[Throwable, List[TableInfo]] = {
    Either.right(
      List(TableInfo("test", "test", "测试"))
    )
  }

  override def listAllColumns(tableId: String): Either[Throwable, List[ColumnInfo]] = {
    Either.cond(tableId == "test", List(ColumnInfo("field1", "field1", "字段1", "string")),
      NoSuchTableError(s"table $tableId not found"))
  }
}
