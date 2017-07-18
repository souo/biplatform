package com.souo.biplatform.queryrouter

import akka.NotUsed
import akka.stream.{Graph, SourceShape}
import com.souo.biplatform.model.QueryModel

/**
 * @author souo
 */
trait ExecutionEngine {
  def execute(query: QueryModel): Graph[SourceShape[DataRow], NotUsed]
}
