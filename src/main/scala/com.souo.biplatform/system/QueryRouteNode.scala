package com.souo.biplatform.system

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.souo.biplatform.model._
import akka.pattern.ask
import com.souo.biplatform.common.api.DefaultTimeOut
import akka.NotUsed
import akka.stream.{Graph, SourceShape}
import com.souo.biplatform.queryrouter.DataRow
import com.souo.biplatform.queryrouter.jdbc.JDBCEngine
import com.souo.biplatform.system.DataSourceNode.Item

import scala.concurrent.{ExecutionContext, Future}
import akka.pattern.pipe
import cats.syntax.either._
import com.souo.biplatform.system.QueryRouteNode.GetDimValues

/**
 * @author souo
 */
class QueryRouteNode(cubeNode: ActorRef, dsNode: ActorRef) extends Actor with ActorLogging with DefaultTimeOut {

  import context.dispatcher

  override def receive: Receive = {
    case query: ReportQuery ⇒
      val result = execute(query)
      result pipeTo sender()

    case GetDimValues(cubeId, tableName, dimName) ⇒
      val result = getDimValues(cubeId, tableName, dimName)
      result pipeTo sender()
  }

  def getCubeSchema(id: UUID)(implicit ec: ExecutionContext): Future[Result[CubeSchema]] = {
    (cubeNode ? CubeNode.Get(id)).mapTo[Result[CubeNameAndSchema]].map{
      _.map(_.schema)
    }
  }

  def getDataSource(schema: Result[CubeSchema])(implicit ec: ExecutionContext): Future[Result[DataSource]] = {
    schema match {
      case Right(s) ⇒
        val id = s.dataSourceId
        val getDs = (dsNode ? DataSourceNode.Get(id)).mapTo[Result[Item]]
        for {
          res ← getDs
        } yield {
          res.map(_.dataSource)
        }

      case Left(t) ⇒
        Future.successful[Result[DataSource]](Left(t))
    }
  }

  def plainQuery(ds: DataSource)(query: QueryModel): Result[Graph[SourceShape[DataRow], NotUsed]] = {
    ds match {
      case jdbc: JdbcSource ⇒
        val engine = new JDBCEngine(jdbc)
        Either.catchNonFatal {
          engine.execute(query)
        }
      case _ ⇒
        Left(new RuntimeException("unSupport datasource"))
    }
  }

  def execute(query: ReportQuery)(implicit ec: ExecutionContext): Future[Result[Graph[SourceShape[DataRow], NotUsed]]] = {
    for {
      schema ← getCubeSchema(query.cubeId)
      ds ← getDataSource(schema)
    } yield {
      ds.flatMap(d ⇒ plainQuery(d)(query.queryModel))
    }
  }

  def getDimValues(cubeId: UUID, tableName: String, dimName: String): Future[Result[List[String]]] = {
    for {
      schema ← getCubeSchema(cubeId)
      ds ← getDataSource(schema)
    } yield {
      ds.flatMap(_.getDimValues(tableName, dimName))
    }
  }

}

object QueryRouteNode {

  def props(cube: ActorRef, dataSource: ActorRef) = {
    Props(classOf[QueryRouteNode], cube, dataSource)
  }

  val name = "query-route"

  case class GetDimValues(cubeId: UUID, tableName: String, dimName: String)
}
