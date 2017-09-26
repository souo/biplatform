package akka.stream.jdbc.impl

import java.sql.{Connection, PreparedStatement, ResultSet}

import akka.actor.{ActorLogging, Props}
import akka.stream.actor.ActorPublisherMessage
import akka.stream.jdbc.JdbcSettings
import cats.syntax.either._
import com.souo.biplatform.queryrouter.DataCellType._
import com.souo.biplatform.queryrouter.{DataCell, DataRow}

import scala.annotation.tailrec
import scala.util.control.NonFatal

/**
 * @author souo
 */
object JDBCPublish {
  def props(settings: JdbcSettings): Props = {
    Props(classOf[JDBCPublish], settings)
  }
}

class JDBCPublish(settings: JdbcSettings)
  extends akka.stream.actor.ActorPublisher[DataRow] with ActorLogging {

  val dbInstance = settings.source

  private var connection: Connection = _
  private var stmt: PreparedStatement = _
  private var rs: ResultSet = _
  private var rowIndex = 0
  private val metaRow = settings.metaRow

  override def preStart() = {
    val res = dbInstance.getConnection.map { conn ⇒
      connection = conn
      val sql = settings.sql
      stmt = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
      val url = settings.source.url
      if (url.startsWith("jdbc:mysql:")) {
        // setFetchSize(Integer.MIN_VALUE) is a mysql driver specific way to force
        // streaming results, rather than pulling entire resultset into memory.
        stmt.setFetchSize(Integer.MIN_VALUE)
      }
      else {
        stmt.setFetchSize(100)
      }
      log.info(s"statement fetch size set to: ${stmt.getFetchSize}")
      stmt
    }.flatMap { stmt ⇒
      Either.catchNonFatal{
        rs = stmt.executeQuery()
        rs
      }
    }

    res.leftMap { t ⇒
      log.error(s"on error then stop")
      t.printStackTrace()
      onErrorThenStop(t)
    }
    super.preStart()
  }

  override def receive: Receive = {
    case ActorPublisherMessage.Request(n) ⇒ send()
    case ActorPublisherMessage.Cancel     ⇒ context.stop(self)
  }

  @tailrec
  private def send(): Unit = {
    if (isActive) {
      if (totalDemand > 0 && rs.next()) {
        var colIndex = 0
        val dataRow = DataRow(metaRow.cells.zipWithIndex.map {
          case (cell, index) ⇒
            val (tpe, props) = cell.`type` match {
              case ROW_HEADER_HEADER ⇒
                (
                  ROW_HEADER,
                  Map("dimension" → cell.value))
              case COLUMN_HEADER ⇒
                colIndex += 1
                (DATA_CELL, Map("position" → s"$rowIndex:$colIndex"))
            }
            DataCell(
              rs.getString(index + 1),
              tpe,
              props)
        })
        onNext(dataRow)
        rowIndex += 1
        send()
      }
      else {
        if (!rs.next()) {
          log.info("no next result, stopping...")
          onCompleteThenStop()
        }
      }
    }
  }

  override def postStop(): Unit = {
    try {
      if (rs ne null) {
        log.info("closing ResultSet...")
        rs.close()
      }
      if (stmt ne null) {
        log.info("closing stmt...")
        stmt.close()
      }
      if (connection ne null) {
        log.info("closing connection...")
        connection.close()
      }
    }
    catch {
      case NonFatal(ex) ⇒
        log.error(s"error when closing, cause:${ex.getCause.getMessage}")
    }
    super.postStop()
  }
}

