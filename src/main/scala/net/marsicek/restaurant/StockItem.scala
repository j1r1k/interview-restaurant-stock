package net.marsicek.restaurant

import io.circe._
import io.circe.generic.semiauto._
import net.marsicek.restaurant.StockItem.StockItemQuantity

case class StockItemId(value: String) extends AnyVal {
  override def toString: String = value
}

object StockItemId {
  implicit val stockItemIdDecoder: Decoder[StockItemId] = Decoder.decodeString.map(StockItemId.apply)
  implicit val stockItemIdEncoder: Encoder[StockItemId] = Encoder.encodeString.contramap(_.value)
}

sealed trait StockItemUpdate {
  def applyOn(stockItem: StockItem): StockItem
}

case class UpdateQuantity(quantity: StockItemQuantity) extends StockItemUpdate {
  override def applyOn(stockItem: StockItem): StockItem = stockItem.copy(quantity = quantity)
}

object StockItemUpdate {
  implicit val stockItemUpdateDecoder: Decoder[StockItemUpdate] = deriveDecoder
  implicit val stockItemUpdateEncoder: Encoder[StockItemUpdate] = deriveEncoder
}

case class StockItem(id: StockItemId, name: String, quantity: StockItemQuantity) {
  /**
    * Override is here mainly to simplify property based tests in `StockActorStorageSpec`
    * @param obj
    * @return
    */
  override def equals(obj: scala.Any): Boolean = obj match {
    case other: StockItem => id.value.toLowerCase == other.id.value.toLowerCase
    case _ => false
  }

  override def hashCode(): Int = id.value.toLowerCase.hashCode
}

object StockItem {
  type StockItemQuantity = Double

  implicit val stockItemDecoder: Decoder[StockItem] = deriveDecoder
  implicit val stockItemEncoder: Encoder[StockItem] = deriveEncoder
}

