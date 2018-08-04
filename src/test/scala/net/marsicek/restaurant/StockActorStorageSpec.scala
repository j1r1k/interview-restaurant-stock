package net.marsicek.restaurant

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.util.Timeout
import org.scalatest.prop.PropertyChecks
import org.scalatest.{AsyncFunSpecLike, BeforeAndAfterAll, FunSpecLike}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class StockActorStoragePropertySpec extends TestKit(ActorSystem("test"))
  with FunSpecLike
  with PropertyChecks
  with BeforeAndAfterAll
  with AwaitAsyncAssertion
  with StockItemGenerators {

  implicit val ec: ExecutionContext = system.dispatcher

  implicit val timeout: Timeout = Timeout(5 seconds)

  override def afterAll(): Unit = {
    shutdown(system)
    super.afterAll()
  }

  describe("StockActorStorage") {
    /**
      * Property checking serve as a blackbox testing of storage subsystem
      */
    describe("properties") {

      it("should be possible to retrieve stored item") {
        val storage = StockActorStorage()

        forAll { item: StockItem =>
          val futureResult: Future[Option[StockItem]] = for {
            _ <- storage.setItem(item)
            retrieved <- storage.getItem(item.id)
          } yield retrieved

          asyncAssert[Option[StockItem]](_.contains(item))(futureResult)
        }
      }

      it("stored item should be present in all items") {
        val storage = StockActorStorage()

        forAll { item: StockItem =>
          val futureResult: Future[Set[StockItem]] = for {
            _ <- storage.setItem(item)
            retrieved <- storage.getAll()
          } yield retrieved

          asyncAssert[Set[StockItem]](_.contains(item))(futureResult)
        }
      }

      it("storing a new item should not alter other items") {
        forAll { (newItem: StockItem, items: Set[StockItem]) =>
          if (items.map(_.id).contains(newItem.id)) {
            succeed
          } else {
            val storage = StockActorStorage(items.toList: _*)

            val futureResult: Future[(Set[StockItem], Set[StockItem])] = for {
              beforeItems <- storage.getAll()
              _ <- storage.setItem(newItem)
              afterItems <- storage.getAll()
            } yield (beforeItems, afterItems)

            asyncAssert[(Set[StockItem], Set[StockItem])] {
              case (beforeItems, afterItems) =>
                afterItems.contains(newItem) && beforeItems == (afterItems - newItem)
            }(futureResult)
          }
        }
      }

      it("storage should be unchanged after store and delete operation") {
        forAll { (newItem: StockItem, items: Set[StockItem]) =>
          if (items.map(_.id).contains(newItem.id)) {
            succeed
          } else {
            val storage = StockActorStorage(items.toList: _*)

            val futureResult: Future[(Set[StockItem], Set[StockItem])] = for {
              beforeItems <- storage.getAll()
              _ <- storage.setItem(newItem)
              _ <- storage.removeItem(newItem.id)
              afterItems <- storage.getAll()
            } yield (beforeItems, afterItems)

            asyncAssert[(Set[StockItem], Set[StockItem])] {
              case (beforeItems, afterItems) =>
                !afterItems.contains(newItem) && beforeItems == afterItems
            }(futureResult)
          }
        }
      }

      it("update operation should not change what items are stored") {
        forAll { (items: Set[StockItem], itemUpdate: StockItemUpdate) =>

          if (items.isEmpty) {
            succeed
          } else {
            val itemList = items.toList
            val storage = StockActorStorage(itemList: _*)

            val itemToUpdate = itemList.head

            val futureResult: Future[(Set[StockItem], Set[StockItem])] = for {
              beforeItems <- storage.getAll()
              _ <- storage.updateItem(itemToUpdate.id, itemUpdate)
              afterItems <- storage.getAll()
            } yield (beforeItems, afterItems)

            asyncAssert[(Set[StockItem], Set[StockItem])] {
              case (beforeItems, afterItems) =>
                beforeItems.map(_.id) == afterItems.map(_.id)
            }(futureResult)
          }
        }
      }
    }
  }
}

class StockActorStorageSpec extends TestKit(ActorSystem("test"))
  with AsyncFunSpecLike
  with PropertyChecks
  with BeforeAndAfterAll
  with StockItemGenerators {

  implicit val ec: ExecutionContext = system.dispatcher

  implicit val timeout: Timeout = Timeout(5 seconds)

  override def afterAll(): Unit = {
    shutdown(system)
    super.afterAll()
  }

  describe("StockActorStorage") {
    it("getAll should return empty set for new storage") {
      val storage = StockActorStorage()
      storage.getAll().map(items => assert(items.isEmpty))
    }

    it("getItem returns None for id that is not associated with item in storage") {
      val storage = StockActorStorage()
      storage.getItem(StockItemId("a1")).map(retrieved => assert(retrieved.isEmpty))
    }

    it("getItem retrieves item stored under an id in storage") {
      val id = StockItemId("a1")
      val item = StockItem(id, "Apple", 10)
      val storage = StockActorStorage(item)

      storage.getItem(id).map(retrieved => assert(retrieved.contains(item)))
    }

    it("setItem stores item under an id in storage") {
      val storage = StockActorStorage()

      val id = StockItemId("a1")
      val item = StockItem(id, "Apple", 10)

      for {
        _ <- storage.setItem(item)
        retrieved <- storage.getItem(id)
        items <- storage.getAll()
      } yield {
        assert(retrieved.contains(item) && items == Set(item))
      }
    }

    it("setItem overwrites item stored under an id in storage if already present") {
      val id = StockItemId("a1")
      val item = StockItem(id, "Apple", 10)
      val storage = StockActorStorage(item)

      val newItem = StockItem(id, "Apricot", 20)

      for {
        _ <- storage.setItem(newItem)
        items <- storage.getAll()
      } yield assert(items == Set(newItem))
    }

    it("removeItem should end with failure for an id that is not associated with item in storage") {
      val storage = StockActorStorage()

      recoverToSucceededIf[ItemNotFoundException] {
        storage.removeItem(StockItemId("a1"))
      }
    }

    it("removeItem removes item stored under an id from storage") {
      val id = StockItemId("a1")
      val item = StockItem(id, "Apple", 10)
      val storage = StockActorStorage(item)

      for {
        _ <- storage.removeItem(id)
        items <- storage.getAll()
      } yield assert(items.isEmpty)
    }

    it("updateItem should end with failure for an id that is not associated with item in storage") {
      val storage = StockActorStorage()

      recoverToSucceededIf[ItemNotFoundException] {
        storage.updateItem(StockItemId("a1"), UpdateQuantity(20))
      }
    }

    it("updateItem should apply update operation over an item in storage stored under an id") {
      val id = StockItemId("a1")
      val item = StockItem(id, "Apple", 10)
      val storage = StockActorStorage(item)

      val newQuantity = 20

      for {
        _ <- storage.updateItem(id, UpdateQuantity(newQuantity))
        items <- storage.getAll()
      } yield assert(items == Set(item.copy(quantity = newQuantity)))
    }
  }
}
