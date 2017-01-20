package com.souo.biplatform.queryrouter.mysql

import cats.{Id, ~>}
import com.souo.biplatform.model._
import com.souo.biplatform.queryrouter.mysql.MysqlQueryBuilder._
import com.souo.biplatform.queryrouter.{PartBuilder, QueryBuilder}
import cats.syntax.either._

/**
  * Created by souo on 2017/1/4
  */
// format: OFF
class MysqlQueryBuilder extends QueryBuilder[Result[List[SqlField]],
  Result[List[SqlField]],
  Result[Option[List[SqlFilter]]],
  Result[SqlResult]] {

  override val queryCompiler: ~>[PartBuilder, Id] = new MysqlQueryCompiler

  override def merge(
                      table: String,
                      select: Result[List[SqlField]],
                      filter: Result[Option[List[SqlFilter]]],
                      groupBy: Result[List[SqlField]]
                    ): Result[SqlResult] = {
    for {
      s ← select
      f ← filter
      g ← groupBy
    } yield {
      val heads = SqlHeads(g, s)
      val sql =
        s"""
           |SELECT ${(g ::: s).map(_.sql).mkString(", ")}
           |FROM  $table
           |${f.filter(_.nonEmpty).fold("") { x ⇒ "WHERE " + x.map(_.sql).mkString(" AND ") }}
           |GROUP BY ${g.map(_.sql).mkString(", ")}
         """.stripMargin.replaceAll("\n", " ")
      SqlResult(heads, sql)
    }
  }
}

// format: ON

object MysqlQueryBuilder {

  case class SqlField(name: String, sqlName: String, caption: String) {
    def sql = sqlName
  }

  case class SqlFilter(name: String, oper: String, value: String) {
    def sql = Seq(name, oper, value).mkString(" ")
  }

  case class SqlHeads(dimHeads: List[SqlField], measureHeads: List[SqlField])

  case class SqlResult(heads: SqlHeads, sql: String)

  type Result[A] = Either[Throwable, A]
}
