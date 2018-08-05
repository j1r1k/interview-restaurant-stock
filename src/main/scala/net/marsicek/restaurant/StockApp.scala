package net.marsicek.restaurant

import akka.actor.ActorSystem
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.{Http, server}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.concurrent.duration._
import scala.language.postfixOps

object StockApp extends App {
  import akka.http.scaladsl.server.Directives.pathPrefix

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val config: Config = ConfigFactory.load()

  /**
    * Config resolution will fail with exceptions if not configured properly,
    *  it will result in termination of the application upon startup
    */
  val interface: String = config.getString("listen.interface")
  val port: Int = config.getInt("listen.port")

  /**
    * Timeout for ask pattern in `StockActorStorage` (should not apply, since it is just single node configuration)
    */
  implicit val databaseTimeout: Timeout = Timeout(30 seconds)

  val api: StockApi = new StockApi(new StockActorStorage())

  val route: server.Route =
    pathPrefix("v1") {
      api.route
    }

  /**
    * Boilerplate from https://doc.akka.io/docs/akka-http/10.1.3/introduction.html?language=scala
    */
  val bindingFuture: Future[ServerBinding] = Http().bindAndHandle(route, interface, port)

  println(s"Server online at http://${interface}:${port}/\nPress RETURN to stop...")

  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}
