package net.marsicek.restaurant

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.ExceptionHandler

import scala.concurrent.ExecutionContext

class StockApi(storage: StockStorage)(implicit val ec: ExecutionContext) {
  import akka.http.scaladsl.server.Directives._
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: ItemNotFoundException =>
      complete(StatusCodes.NotFound, e.getMessage)
  }

  val route: server.Route =
    handleExceptions(exceptionHandler) {
      pathPrefix(StockApi.PREFIX) {
        pathEnd {
          get {
            complete(storage.getAll())
          }
        } ~
          path(Segment.map(StockItemId.apply)) { id =>
            get {
              rejectEmptyResponse {
                pathEnd {
                  complete(storage.getItem(id))
                } ~
                  path("quantity") {
                    complete(storage.getItem(id).map(_.map(_.quantity)))
                  }
              }
            } ~
              delete {
                complete(storage.removeItem(id))
              } ~
              put {
                entity(as[StockItem]) { stockItem =>
                  complete(
                    storage.setItem(stockItem).map(_ => StatusCodes.Created)
                  )
                }
              } ~
              patch {
                entity(as[StockItemUpdate]) { stockItemUpdate =>
                  complete(storage.updateItem(id, stockItemUpdate))
                }
              }
          }
      }
    }
}

object StockApi {
  val PREFIX = "items"
}
