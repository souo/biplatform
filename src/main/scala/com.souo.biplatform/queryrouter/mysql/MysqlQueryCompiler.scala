
package com.souo.biplatform.queryrouter.mysql

import cats.syntax.either._
import cats.{Id, ~>}
import com.souo.biplatform.model._
import com.souo.biplatform.queryrouter.mysql.MysqlQueryBuilder._
import com.souo.biplatform.queryrouter._

/**
 * Created by souo on 2017/1/4
 */
class MysqlQueryCompiler extends (PartBuilder ~> Id) {

  val nameMaps = Map("SUM" → "求和", "COUNT" → "计数", "AVG" → "平均值")

  override def apply[A](fa: PartBuilder[A]): Id[A] = fa match {
    case MeasuresBuilder(measures) ⇒
      Either.catchNonFatal {
        measures.map {
          case Measure(dim, aggregator) ⇒
            aggregator match {
              case "DISTINCT_COUNT" ⇒
                val sqlName = s"COUNT(DISTINCT(${dim.name}))"
                val caption = {
                  if (dim.caption.isEmpty) {
                    dim.name
                  }
                  else {
                    dim.caption
                  }
                } + "去重计数"
                SqlField(dim.name, sqlName, caption)
              case "SUM" | "COUNT" | "AVG" ⇒
                val sqlName = s"$aggregator(${dim.name})"
                val caption = {
                  if (dim.caption.isEmpty) {
                    dim.name
                  }
                  else {
                    dim.caption
                  }
                } + nameMaps(aggregator)
                SqlField(dim.name, sqlName, caption)
              case _ ⇒
                sys.error(s"UnSupport measure aggregator $aggregator")
            }
        }
      }.asInstanceOf[A]

    case DimensionsBuilder(dimensions) ⇒
      Either.right[Throwable, Seq[SqlField]] {
        dimensions.map(d ⇒ SqlField(d.name, d.name, d.caption))
      }.asInstanceOf[A]

    case FilterBuilder(filters) ⇒
      Either.catchNonFatal {
        filters.filter(_.nonEmpty).map {
          _.map {
            case gt(dim, value) ⇒
              SqlFilter(dim.name, ">", value)
            case gte(dim, value) ⇒
              SqlFilter(dim.name, ">=", value)
            case lt(dim, value) ⇒
              SqlFilter(dim.name, "<", value)
            case lte(dim, value) ⇒
              SqlFilter(dim.name, "<=", value)
            case eql(dim, value) ⇒
              SqlFilter(dim.name, "=", value)
            case in(dim, value) ⇒
              SqlFilter(dim.name, "IN", s"(${value.mkString(", ")})")
            case range(dim, value) if value.length == 2 ⇒
              SqlFilter(dim.name, "", s"BETWEEN ${value(0)} AND ${value(1)}")
            case other ⇒
              sys.error(s"wrong filter $other")
          }
        }
      }.asInstanceOf[A]
  }
}
