package com.souo.biplatform.queryrouter.mysql

import java.util.concurrent.atomic.AtomicInteger

import akka.NotUsed
import akka.stream.{FlowShape, Graph, Outlet, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Merge, Source, Zip}
import cats.data.Validated.{Invalid, Valid}
import com.typesafe.scalalogging.StrictLogging
import com.souo.biplatform.model.QueryModel
import com.souo.biplatform.queryrouter.DataCellType._
import com.souo.biplatform.queryrouter._
import com.souo.biplatform.queryrouter.mysql.MysqlQueryBuilder.SqlResult
import slick.jdbc.GetResult
import slick.driver.MySQLDriver.api._

/**
 * Created by souo on 2017/1/5
 */
class MysqlEngine(storage: MysqlStorage) extends ExecutionEngine with StrictLogging {

  val queryBuilder = new MysqlQueryBuilder

  private def getResult[Result >: GetResult[DataRow]](metaRow: DataRow, rowIndexInc: () ⇒ Int): Result = GetResult { r ⇒
    val rowIndex = rowIndexInc()
    var colIndex = 0
    DataRow(metaRow.cells.map { cell ⇒
      val (tpe, props) = cell.`type` match {
        case ROW_HEADER_HEADER ⇒
          (
            ROW_HEADER,
            Map("dimension" → cell.value)
          )
        case COLUMN_HEADER ⇒
          colIndex += 1
          (DATA_CELL, Map("position" → s"$rowIndex:$colIndex"))
      }

      DataCell(
        r.nextString(),
        tpe,
        props
      )
    })
  }

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

      val F = builder.add(Flow[(SqlResult, DataRow)].flatMapConcat {
        case (r, d) ⇒
          GraphDSL.create[SourceShape[DataRow]]() { implicit b ⇒
            import GraphDSL.Implicits._
            val rowIndex = new AtomicInteger(0)
            val sql =
              sql"""#${r.sql}""".as(getResult(d, () ⇒ {
                rowIndex.incrementAndGet()
              }))
            val head = Source.single(d)
            val body = Source.fromPublisher(storage.client.stream(sql))
            val q = b.add(head.concat(body))
            SourceShape(q.out)
          }
      })
      // format: OFF

                C ~> D ~> E.in1
      A ~> B ~> C    ~>   E.in0
                          E.out ~> F

      // format: ON

      SourceShape(F.out)
    }
  }
}

