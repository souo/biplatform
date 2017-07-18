package akka.stream.jdbc.impl

import akka.NotUsed
import akka.stream.{ActorMaterializerHelper, Attributes, MaterializationContext, SourceShape}
import akka.stream.impl.{SourceModule, StreamLayout}
import akka.stream.jdbc.JdbcSettings
import com.souo.biplatform.queryrouter.DataRow
import org.reactivestreams.Publisher

/**
 * @author souo
 */
private[akka] class JDBCSource(settings: JdbcSettings, val attributes: Attributes, shape: SourceShape[DataRow])
    extends SourceModule[DataRow, NotUsed](shape) {

  override def create(context: MaterializationContext): (Publisher[DataRow], NotUsed) = {
    val materializer = ActorMaterializerHelper.downcast(context.materializer)
    val props = JDBCPublish.props(settings)
    val ref = materializer.actorOf(context, props)
    (akka.stream.actor.ActorPublisher[DataRow](ref), NotUsed)
  }

  override protected def newInstance(shape: SourceShape[DataRow]): SourceModule[DataRow, NotUsed] = {
    new JDBCSource(settings, attributes, shape)
  }

  override def withAttributes(attributes: Attributes): StreamLayout.Module = {
    new JDBCSource(settings, attributes, amendShape(attributes))
  }
}
