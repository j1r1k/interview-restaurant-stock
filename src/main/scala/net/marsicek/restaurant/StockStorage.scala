package net.marsicek.restaurant

import scala.concurrent.Future

trait StockStorage {

  def setItem(item: StockItem): Future[Unit]

  def getItem(id: StockItemId): Future[Option[StockItem]]

  def removeItem(id: StockItemId): Future[Unit]

  def updateItem(id: StockItemId, update: StockItemUpdate): Future[StockItem]

  def getAll(): Future[Set[StockItem]]

}

case class ItemNotFoundException(id: StockItemId) extends IllegalArgumentException(s"Item with id: '$id' not found")