package com.souo.biplatform.queryrouter

import akka.NotUsed
import akka.stream.{Graph, SourceShape}
import akka.stream.scaladsl.Source
import com.souo.biplatform.model.QueryModel

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by souo on 2017/1/5
 */
trait ExecutionEngine {
  def execute(query: QueryModel): Graph[SourceShape[DataRow], NotUsed]
}
