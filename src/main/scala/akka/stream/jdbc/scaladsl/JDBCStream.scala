package akka.stream.jdbc.scaladsl

import akka.stream.Attributes
import akka.stream.jdbc.JdbcSettings
import akka.stream.jdbc.impl.JDBCSource
import akka.stream.scaladsl.Source
import Source.{shape ⇒ sourceShape}
import akka.NotUsed
import com.souo.biplatform.queryrouter.DataRow

/**
 * @author souo
 */
object JDBCStream {

  def plainSource(settings: JdbcSettings): Source[DataRow, NotUsed] = {
    new Source(new JDBCSource(
      settings,
      Attributes.none,
      sourceShape("JdbcSource")
    ))
  }

}
