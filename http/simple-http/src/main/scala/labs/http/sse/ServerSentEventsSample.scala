package labs.http.sse

import java.time.LocalTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source

import scala.concurrent.duration._
import scala.util.{Success, Failure}


/**
 *
 * https://doc.akka.io/docs/akka-http/current/common/sse-support.html
 *
 * Test:
 * $ curl http://localhost:8080/events
 *
 */
object ServerSentEventsSample {

  def main(args: Array[String]): Unit = {
    val behavior = rootBehavior(routes)
    ActorSystem[Nothing](behavior, "sse-server")
  }

  private def routes: Route =
    path("events") {
      import CORSHandler._
      import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
      get {
        corsHandler(
          complete {
            Source
                    .tick(2.seconds, 2.seconds, NotUsed)
                    .map(_ => LocalTime.now())
                    .map(time => ServerSentEvent(ISO_LOCAL_TIME.format(time)))
                    .keepAlive(1.second, () => ServerSentEvent.heartbeat)
          }
        )
      }
    }


  private def rootBehavior(routes: Route) = Behaviors.setup[Nothing] { context =>
    startHttpServer(routes)(context.system)
    Behaviors.empty
  }

  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext
    val binding = Http().newServerAt("localhost", 8080).bind(routes)
    binding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

}
