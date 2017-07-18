package akka.stream.jdbc

import com.souo.biplatform.model.JdbcSource
import com.souo.biplatform.queryrouter.DataRow

/**
 * @author souo
 */
case class JdbcSettings(
  source:  JdbcSource,
  sql:     String,
  metaRow: DataRow
)
