package com.souo.biplatform.queryrouter.jdbc

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.jdbc.JdbcSettings
import akka.stream.jdbc.scaladsl.JDBCStream
import akka.stream.scaladsl._
import cats.data.Validated.{Invalid, Valid}
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.model._
import com.souo.biplatform.queryrouter.DataCellType._
import com.souo.biplatform.queryrouter.jdbc.QueryBuilder.SqlResult
import com.souo.biplatform.queryrouter.{DataCell, DataRow, ExecutionEngine}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * @author souo
 */
class JDBCEngine(dataSource: JdbcSource) extends ExecutionEngine with StrictLogging {

  val queryBuilder = new QueryBuilder

  override def execute(query: QueryModel): Graph[SourceShape[DataRow], NotUsed] = {
    GraphDSL.create() { implicit builder ⇒
      import GraphDSL.Implicits._
      //check
      val A: Outlet[Boolean] = {
        val source = {
          query.check() match {
            case Invalid(l) ⇒
              Source.failed[Boolean](new RuntimeException(l.toList.mkString(";")))
            case Valid(v) ⇒
              Source.single[Boolean](v)
          }
        }
        builder.add(source).out
      }
      //compile
      val B: FlowShape[Boolean, SqlResult] = {
        builder.add(
          Flow[Boolean].map {
            case true ⇒
              queryBuilder.build(query).fold(throw _, identity)
            case false ⇒
              sys.error("")
          })
      }

      //query
      val C = builder.add(Flow[SqlResult].flatMapConcat { r ⇒
        val metaRow = {
          DataRow(r.heads.dimHeads.map { f ⇒
            DataCell(f.name, ROW_HEADER_HEADER, Map(
              "caption" → f.caption))
          } ::: r.heads.measureHeads.map { f ⇒
            DataCell(f.name, COLUMN_HEADER, Map(
              "caption" → f.caption))
          })
        }
        val head = Source.single(metaRow)
        val settings = JdbcSettings(
          dataSource,
          r.sql,
          metaRow)
        val body = JDBCStream.plainSource(settings)
        head.concat(body)
      })
      A ~> B ~> C
      SourceShape(C.out)
    }
  }
}

