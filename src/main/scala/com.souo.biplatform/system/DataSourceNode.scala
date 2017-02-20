package com.souo.biplatform.system

import java.util.UUID

import akka.actor.Props
import akka.persistence.RecoveryCompleted
import com.souo.biplatform.model.DataSource
import com.souo.biplatform.system.DataSourceNode.Items
import com.souo.biplatform.system.DataSourceNode._

/**
 * Created by souo on 2017/1/24
 */
class DataSourceNode extends Node {
  var items = Items()

  def updateItems(evt: Event) = evt match {
    case Added(item) ⇒
      items = items.add(item)
    case Removed(dsId) ⇒
      items = items.remove(dsId)
  }

  override def receiveRecover: Receive = {
    case evt: Event ⇒
      updateItems(evt)
    case RecoveryCompleted ⇒
      log.info("Recover completed for {} in {}ms", persistenceId, System.currentTimeMillis() - created)
  }

  override def receiveCommand: Receive = {
    case Add(name, ds) ⇒
      val dsId = UUID.randomUUID()
      val item = Item(dsId, name, ds)
      val replyTo = sender()
      persistAsync(Added(item)){ evt ⇒
        updateItems(evt)
        replyTo ! item
      }

    case Remove(id) ⇒
      items.items.find(_.dsId == id) match {
        case Some(item) ⇒
          val replyTo = sender()
          persistAsync(Removed(id)){ evt ⇒
            updateItems(evt)
            replyTo ! None
          }
        case None ⇒
          sender() ! NoSuchDS
      }

    case ListAllDS ⇒
      sender() ! items

    case Get(id) ⇒
      sender() ! items.items.find(_.dsId == id)
  }
}

object DataSourceNode {

  def props = Props(classOf[DataSourceNode])
  val name = "datasource"

  case class Item(dsId: UUID, name: String, dataSource: DataSource)
  case class Items(items: List[Item] = List.empty) {
    def add(item: Item) = {
      copy(items = items :+ item)
    }

    def remove(dsId: UUID) = {
      copy(items = items.filterNot(_.dsId == dsId))
    }
  }

  trait Command
  case class Add(name: String, dataSource: DataSource) extends Command
  case class Remove(dsId: UUID) extends Command

  trait Event
  case class Added(item: Item) extends Event
  case class Removed(dsId: UUID) extends Event

  case object NoSuchDS
  case object ListAllDS
  case class Get(dsId: UUID)

}
