package com.souo.biplatform.queryrouter.mysql

import java.util.concurrent.atomic.AtomicInteger

import akka.stream.SourceShape
import akka.stream.scaladsl.{Flow, GraphDSL, Source}
import com.souo.biplatform.model.MysqlSource
import com.souo.biplatform.queryrouter.DataCellType._
import com.souo.biplatform.queryrouter.{DataCell, DataRow}
import com.souo.biplatform.queryrouter.mysql.MysqlQueryBuilder.SqlResult
import slick.driver.MySQLDriver.api._
import slick.jdbc.GetResult
import cats.syntax.either._

/**
  * Created by souo on 2017/1/5
  */
class MysqlStorage(source: MysqlSource) {

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

  def flow = Flow[(SqlResult, DataRow)].flatMapConcat {
    case (r, d) ⇒
      GraphDSL.create[SourceShape[DataRow]]() { implicit b ⇒
        import GraphDSL.Implicits._
        val rowIndex = new AtomicInteger(0)
        val sql =
          sql"""#${r.sql}""".as(getResult(d, () ⇒ {
            rowIndex.incrementAndGet()
          }))
        val head = Source.single(d)
        val publish = source.withDB { db =>
          Either.catchNonFatal(
            db.stream(sql)
          )
        }

        val body = publish match {
          case Left(t) =>
            Source.failed(t)
          case Right(p) =>
            Source.fromPublisher(p)
        }

        val q = b.add(head.concat(body))
        SourceShape(q.out)
      }
  }
}
