package com.souo.biplatform.queryrouter.mysql

import java.util.concurrent.atomic.AtomicInteger

import akka.NotUsed
import akka.stream.{FlowShape, Graph, Outlet, SourceShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Source, Zip}
import cats.data.Validated.{Invalid, Valid}
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.model.{Dimension, Measure, MysqlSource, QueryModel}
import com.souo.biplatform.queryrouter.DataCellType._
import com.souo.biplatform.queryrouter._
import com.souo.biplatform.queryrouter.mysql.MysqlQueryBuilder.SqlResult
import slick.jdbc.GetResult
import slick.driver.MySQLDriver.api._

/**
 * Created by souo on 2017/1/5
 */
class MysqlEngine(source:MysqlSource) extends ExecutionEngine with StrictLogging {

  val queryBuilder = new MysqlQueryBuilder

  val storege  = new MysqlStorage(source)

  override def execute(query: QueryModel): Graph[SourceShape[DataRow], NotUsed] = {
    GraphDSL.create() { implicit builder ⇒
      import GraphDSL.Implicits._

      //check
      val A: Outlet[Boolean] = {
        val source = {
          query.check() match {
            case Invalid(s) ⇒
              Source.failed[Boolean](new RuntimeException(s))
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
          }
        )
      }

      //broadCast
      val C = builder.add(Broadcast[SqlResult](2))

      //firstRow
      val D = Flow[SqlResult].map { r ⇒
        DataRow(r.heads.dimHeads.map { f ⇒
          DataCell(f.name, ROW_HEADER_HEADER, Map(
            "caption" → f.caption
          ))
        } ::: r.heads.measureHeads.map { f ⇒
          DataCell(f.name, COLUMN_HEADER, Map(
            "caption" → f.caption
          ))
        })
      }

      val E = builder.add(Zip[SqlResult, DataRow])

      val F =  builder.add(storege.flow)
      // format: OFF

                C ~> D ~> E.in1
      A ~> B ~> C    ~>   E.in0
                          E.out ~> F

      // format: ON

      SourceShape(F.out)
    }
  }
}

object MysqlEngine extends App{

  val engine = new MysqlEngine(MysqlSource("10.209.44.12", 10043, "wanda", "wanda", "wanda"))
  val queryModel = QueryModel(
    "app_visit_week",
    List(
      Dimension("visit_type", "", None, "string"),
      Dimension("source", "", None, "string")
    ),
    List(
      Measure(Dimension("uv", "", None, "string"), "SUM"),
      Measure(Dimension("pv", "", None, "string"), "COUNT")
    ),
    Some(
      List()
    )
  )

}

