package net.marsicek.restaurant

import akka.Done
import akka.actor.Status.Failure
import akka.actor.{Actor, ActorSystem, Props}
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}

sealed trait StockItemDatabaseActorProtocol

case class SetItem(item: StockItem) extends StockItemDatabaseActorProtocol
case class GetItem(id: StockItemId) extends StockItemDatabaseActorProtocol
case class RemoveItem(id: StockItemId) extends  StockItemDatabaseActorProtocol
case class UpdateItem(id: StockItemId, update: StockItemUpdate) extends StockItemDatabaseActorProtocol
case object GetAll extends StockItemDatabaseActorProtocol

class StockActorStorage(initialState: Map[StockItemId, StockItem] = Map.empty)
                       (implicit system: ActorSystem, timeout: Timeout, ec: ExecutionContext) extends StockStorage {
  import akka.pattern.ask

  private val databaseActor = system.actorOf(StockDatabaseActor.props(initialState))

  override def setItem(item: StockItem): Future[Unit] =
    ask(databaseActor, SetItem(item)).mapTo[Done].map(_ => {})

  override def getItem(id: StockItemId): Future[Option[StockItem]] =
    ask(databaseActor, GetItem(id)).mapTo[Option[StockItem]]

  override def removeItem(id: StockItemId): Future[Unit] =
    ask(databaseActor, RemoveItem(id)).mapTo[Done].map(_ => {})

  override def updateItem(id: StockItemId, update: StockItemUpdate): Future[StockItem] =
    ask(databaseActor, UpdateItem(id, update)).mapTo[StockItem]

  override def getAll(): Future[Set[StockItem]] =
    ask(databaseActor, GetAll).mapTo[Set[StockItem]]
}

object StockActorStorage {
  def apply(initialStockItems: StockItem*)
           (implicit system: ActorSystem, timeout: Timeout, ec: ExecutionContext): StockActorStorage =
    new StockActorStorage(initialStockItems.map(item => item.id -> item).toMap)
}

class StockDatabaseActor(initialItems: Map[StockItemId, StockItem]) extends Actor {

  def buildReceive(items: Map[StockItemId, StockItem]): Receive = {
    case SetItem(item) =>
      context.become(buildReceive(items + (item.id -> item)))
      sender() ! Done
    case GetItem(id) =>
      sender() ! items.get(id)
    case RemoveItem(id) =>
      if (items.contains(id)) {
        context.become(buildReceive(items - id))
        sender() ! Done
      } else {
        sender() ! Failure(ItemNotFoundException(id))
      }
    case UpdateItem(id, update) =>
      items.get(id).map(update.applyOn) match {
        case Some(updatedItem) =>
          context.become(buildReceive(items + (id -> updatedItem)))
          sender() ! updatedItem
        case None =>
          sender() ! Failure(ItemNotFoundException(id))
      }
    case GetAll =>
      sender() ! items.values.toSet
  }

  override def receive: Receive = buildReceive(initialItems)

}

object StockDatabaseActor {
  def props(initialState: Map[StockItemId, StockItem]): Props = Props(new StockDatabaseActor(initialState))
}
