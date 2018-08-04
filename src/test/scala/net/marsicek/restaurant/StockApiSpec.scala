package net.marsicek.restaurant

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.circe.parser.decode
import io.circe.syntax._
import org.scalatest.FunSpec

import scala.collection.mutable
import scala.concurrent.Future

class StockApiSpec extends FunSpec with ScalatestRouteTest {

  /**
    * Mock storage that uses mutable state as internal representation of state
    * @param state initial state
    */
  case class StockVarStorage(var state: mutable.Map[StockItemId, StockItem] = mutable.Map.empty) extends StockStorage {

    override def setItem(item: StockItem): Future[Unit] =
      Future.successful(
        synchronized {
          state.put(item.id, item)
          ()
        }
      )

    override def getItem(id: StockItemId): Future[Option[StockItem]] =
      Future.successful(
        state.get(id)
      )

    override def removeItem(id: StockItemId): Future[Unit] =
      synchronized(state.remove(id)) match {
          case Some(_) => Future.successful()
          case None => Future.failed(ItemNotFoundException(id))
        }

    override def updateItem(id: StockItemId, update: StockItemUpdate): Future[StockItem] =
          state.get(id).map(update.applyOn) match {
            case Some(updatedItem) => Future.successful {
              synchronized(state.update(id, updatedItem))
              updatedItem
            }
            case None => Future.failed(ItemNotFoundException(id))
          }

    override def getAll(): Future[Set[StockItem]] =
      Future.successful(state.values.toSet)
  }

  object StockVarStorage {
    def apply(initialStockItems: (StockItemId, StockItem)*): StockVarStorage =
      StockVarStorage(mutable.Map(initialStockItems:_*))
  }

  def getItem(id: StockItemId): HttpRequest = Get(s"/${StockApi.PREFIX}/$id")

  def putItem(item: StockItem): HttpRequest = HttpRequest(
    method = HttpMethods.PUT,
    uri = s"/${StockApi.PREFIX}/${item.id}",
    entity = HttpEntity(ContentTypes.`application/json`, item.asJson.toString)
  )

  def patchItem(id: StockItemId, update: StockItemUpdate): HttpRequest = HttpRequest(
    method = HttpMethods.PATCH,
    uri = s"/${StockApi.PREFIX}/$id",
    entity = HttpEntity(ContentTypes.`application/json`, update.asJson.toString)
  )

  describe("StockApi") {

    describe("Multiple items API") {
      it("get should return empty list if storage is empty") {
        val api = new StockApi(StockVarStorage())

        Get(s"/${StockApi.PREFIX}") ~> Route.seal(api.route) ~> check {
          assert(status == StatusCodes.OK)
          assert(decode[Seq[StockItem]](responseAs[String]).contains(Seq.empty))
        }
      }

      it("get should return list of all items in storage") {
        val items = Seq(
          StockItem(StockItemId("a1"), "Apple", 10),
          StockItem(StockItemId("a2"), "Apricot", 15),
          StockItem(StockItemId("b1"), "Banana", 20)
        )

        val api = new StockApi(StockVarStorage(items.map(item => item.id -> item):_*))

        Get(s"/${StockApi.PREFIX}") ~> Route.seal(api.route) ~> check {
          assert(status == StatusCodes.OK)
          assert(decode[Seq[StockItem]](responseAs[String]).map(_.toSet).contains(items.toSet))
        }
      }
    }

    describe("Single item API") {

      it("get should return 'Not Found' when requesting unavailable item") {
        val api = new StockApi(StockVarStorage())

        getItem(StockItemId("1")) ~> Route.seal(api.route) ~> check {
          assert(status == StatusCodes.NotFound)
        }
      }

      it("get should return StockItem stored under id") {
        val id = StockItemId("a1")
        val item = StockItem(id, "Apple", 10)
        val api = new StockApi(StockVarStorage(id -> item))

        getItem(id) ~> Route.seal(api.route) ~> check {
          assert(decode[StockItem](responseAs[String]).contains(item))
        }
      }

      it("put should store the item under id in storage") {
        val storage = StockVarStorage()
        val api = new StockApi(storage)

        val id = StockItemId("a1")
        val item = StockItem(id, "Apple", 10)
        putItem(item) ~> Route.seal(api.route) ~> check {
          assert(status == StatusCodes.Created)
          assert(storage.state.get(id).contains(item))
        }
      }

      it("put should overwrite the item under id in storage") {
        val id = StockItemId("a1")
        val item = StockItem(id, "Apple", 10)

        val storage = StockVarStorage(id -> item)
        val api = new StockApi(storage)

        val newItem = StockItem(id, "Apricot", 20)

        putItem(newItem) ~> Route.seal(api.route) ~> check {
          assert(status == StatusCodes.Created)
          assert(storage.state.get(id).contains(newItem))
        }
      }

      it("delete should end with failure for non-existing id") {
        val api = new StockApi(StockVarStorage())

        Delete(s"/${StockApi.PREFIX}/a1") ~> Route.seal(api.route) ~> check {
          assert(status == StatusCodes.NotFound)
        }
      }

      it("delete should remove the item under id from storage") {
        val id = StockItemId("a1")
        val item = StockItem(id, "Apple", 10)
        val storage = StockVarStorage(id -> item)
        val api = new StockApi(storage)

        Delete(s"/${StockApi.PREFIX}/$id") ~> Route.seal(api.route) ~> check {
          assert(status == StatusCodes.OK)
          assert(!storage.state.contains(id))
        }
      }

      it("patch should end with failure for non-existing id") {
        val api = new StockApi(StockVarStorage())

        patchItem(StockItemId("a1"), UpdateQuantity(42)) ~> Route.seal(api.route) ~> check {
          assert(status == StatusCodes.NotFound)
        }
      }


      it("patch should modify the item under id in storage") {
        val id = StockItemId("a1")
        val item = StockItem(id, "Apple", 10)
        val storage = StockVarStorage(id -> item)
        val api = new StockApi(storage)

        val newQuantity = 42

        patchItem(id, UpdateQuantity(newQuantity)) ~> Route.seal(api.route) ~> check {
          assert(decode[StockItem](responseAs[String]).contains(item.copy(quantity = newQuantity)))
          assert(status == StatusCodes.OK)
          assert(storage.state.get(id).map(_.quantity).contains(newQuantity))
        }
      }
    }

    describe("Single item quantity field API") {
      it("get should return 'Not Found' when requesting quantity for an unavailable item") {
        val api = new StockApi(StockVarStorage())

        Get(s"/${StockApi.PREFIX}/a1/quantity") ~> Route.seal(api.route) ~> check {
          assert(status == StatusCodes.NotFound)
        }
      }

    }
  }
}
