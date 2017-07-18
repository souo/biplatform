package com.souo.biplatform.queryrouter

import cats.free.Free
import cats.{Id, ~>}
import com.souo.biplatform.model._
/**
 * @author souo
 */
trait QueryBuilder[M, D, F, T] {
  import QueryBuilder._

  val queryCompiler: PartBuilder ~> Id

  def build(obj: QueryModel): T = {
    val query = for {
      select ← buildMeasures(obj.measures)
      groupBy ← buildDimensions(obj.dimensions)
      filter ← buildFilters(obj.filters)
    } yield {
      merge(obj.tableName, select, filter, groupBy)
    }

    query.foldMap(queryCompiler)
  }

  def buildMeasures(measures: List[Measure]): Part[M] = {
    api.execute(MeasuresBuilder[M](measures))
  }

  def buildDimensions(dimensions: List[Dimension]): Part[D] = {
    api.execute(DimensionsBuilder[D](dimensions))
  }

  def buildFilters(filters: Option[List[Filter]]): Part[F] = {
    api.execute(FilterBuilder[F](filters))
  }

  def merge(table: String, select: M, filter: F, groupBy: D): T
}

object QueryBuilder {
  type Part[A] = Free[PartBuilder, A]
}

sealed trait PartBuilder[T]
case class MeasuresBuilder[M](measures: List[Measure]) extends PartBuilder[M]
case class DimensionsBuilder[D](dimensions: List[Dimension]) extends PartBuilder[D]
case class FilterBuilder[F](filters: Option[List[Filter]]) extends PartBuilder[F]

