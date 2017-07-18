package com.souo.biplatform.system

import java.util.UUID

import akka.actor.{ActorRef, Props}
import akka.persistence.RecoveryCompleted
import DataSourceNode._
import com.souo.biplatform.model._
import cats.syntax.either._
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import org.joda.time.DateTime

import scala.annotation.meta.field

/**
 * @author souo
 */
class DataSourceNode extends Node {
  var items = Items()

  def updateItems(evt: Event) = evt match {
    case Added(item) ⇒
      items = items.add(item)
    case Updated(id, item) ⇒
      items = items.update(id, item)
    case Removed(dsId) ⇒
      items = items.remove(dsId)
  }

  def updateAndReply(evt: Event)(replyTo: ActorRef) = {
    val res = Either.catchNonFatal(
      updateItems(evt)
    )
    replyTo ! res
  }

  override def receiveRecover: Receive = {
    case evt: Event ⇒
      updateItems(evt)
    case RecoveryCompleted ⇒
      log.info("Recover completed for {} in {}ms", persistenceId, System.currentTimeMillis() - created)
  }

  def checkDataSource(ds: DataSource) = {
    ds.check()
  }

  override def receiveCommand: Receive = {
    case Add(user, name, ds) ⇒
      val checkNameAndConnection = for {
        _ ← items.checkName(name)
        _ ← checkDataSource(ds)
      } yield {}

      checkNameAndConnection match {
        case Right(_) ⇒
          val dsId = UUID.randomUUID()
          val item = Item(dsId = dsId, name = name, createBy = user, lastModifyTime = DateTime.now(), dataSource = ds)
          val replyTo = sender()
          persistAsync(Added(item)) { evt ⇒
            updateAndReply(evt)(replyTo)
          }
        case l @ Left(_) ⇒
          sender() ! l
      }

    case Update(user, name, id, ds) ⇒
      val otherItems = items.remove(id)
      val checkNameAndConnection = for {
        _ ← otherItems.checkName(name)
        _ ← checkDataSource(ds)
      } yield {}

      checkNameAndConnection match {
        case Right(_) ⇒
          items.get(id) match {
            case Some(old) ⇒
              val item = old.copy(
                name           = name,
                modifyBy       = Some(user),
                lastModifyTime = DateTime.now()
              )
              val replyTo = sender()
              persistAsync(Updated(id, item)) { evt ⇒
                updateAndReply(evt)(replyTo)
              }
            case None ⇒
              sender() ! Left(new RuntimeException("数据源不存在"))
          }
        case l @ Left(_) ⇒
          sender() ! l
      }

    case Remove(id) ⇒
      val replyTo = sender()
      items.items.find(_.dsId == id) match {
        case Some(_) ⇒
          persistAsync(Removed(id)) { evt ⇒
            updateAndReply(evt)(replyTo)
          }

        case None ⇒
          sender() ! Left(new RuntimeException(s"no such datasource $id"))
      }

    case ListAllDS(query) ⇒
      val res = Either.catchNonFatal{
        query match {
          case Some(q) ⇒
            items.items.filter(_.name.contains(q))
          case None ⇒
            items.items
        }
      }
      sender() ! res

    case ListAllTables(id) ⇒
      val res = Either.fromOption[Throwable, Item](
        items.items.find(_.dsId == id),
        new RuntimeException(s"no such datasource $id")
      ).flatMap {
          _.dataSource.listAllTables()
        }
      sender() ! res

    case ListAllColumns(id, table) ⇒
      val res = Either.fromOption[Throwable, Item](
        items.items.find(_.dsId == id),
        new RuntimeException(s"no such datasource $id")
      ).flatMap {
          _.dataSource.listAllColumns(table)
        }
      sender() ! res

    case Get(id) ⇒
      val res = Either.fromOption(
        items.items.find(_.dsId == id),
        new RuntimeException(s"no such datasource $id")
      )
      sender() ! res
  }
}

object DataSourceNode {

  def props = Props(classOf[DataSourceNode])

  val name = "datasource"

  @ApiModel
  @ApiModelProperty()
  case class Item(
    @(ApiModelProperty @field) dsId:           UUID,
    @(ApiModelProperty @field) name:           String,
    @(ApiModelProperty @field) createBy:       String,
    @(ApiModelProperty @field) modifyBy:       Option[String] = None,
    @(ApiModelProperty @field) lastModifyTime: DateTime,
    @ApiModelProperty(hidden = true) dataSource:DataSource
  )

  case class Items(items: List[Item] = List.empty) {
    def add(item: Item) = {
      copy(items = items :+ item)
    }

    def update(id: UUID, item: Item): Items = {
      val idx = items.indexWhere(_.dsId == id)
      if (idx >= 0) {
        copy(items = items.updated(idx, item))
      }
      else {
        this
      }
    }

    def remove(dsId: UUID) = {
      copy(items = items.filterNot(_.dsId == dsId))
    }

    def get(id: UUID): Option[Item] = {
      items.find(_.dsId == id)
    }

    def checkName(name: String): Result[Unit] = {
      Either.cond(
        !items.exists(_.name == name),
        (),
        InValidName("名称不能重复")
      )
    }
  }

  trait Command

  case class Add(user: String, name: String, dataSource: DataSource) extends Command

  case class Update(user: String, name: String, id: UUID, dataSource: DataSource) extends Command

  case class Remove(dsId: UUID) extends Command

  trait Event

  case class Added(item: Item) extends Event

  case class Updated(dsId: UUID, item: Item) extends Event

  case class Removed(dsId: UUID) extends Event

  case class ListAllDS(query: Option[String])

  case class ListAllTables(id: UUID)

  case class ListAllColumns(id: UUID, table: String)

  case class Get(dsId: UUID)

  case class InValidName(msg: String) extends RuntimeException

}
