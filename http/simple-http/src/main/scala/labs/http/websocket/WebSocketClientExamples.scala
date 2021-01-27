package labs.http.websocket

/**
 *
 * https://github.com/akka/akka-http/blob/v10.2.3/docs/src/test/scala/docs/http/scaladsl/WebSocketClientFlow.scala
 *
 */
object WebSocketClientFlow {
  import akka.Done
  import akka.http.scaladsl.Http
  import akka.http.scaladsl.model.StatusCodes
  import akka.http.scaladsl.model.ws.{WebSocketUpgradeResponse, Message, WebSocketRequest, TextMessage}
  import akka.stream.scaladsl.{Sink, Keep, Source}
  import akka.actor.typed.ActorSystem
  import akka.actor.typed.scaladsl.Behaviors

  import scala.concurrent.Future
  import scala.util.{Success, Failure}

  implicit val system = ActorSystem(Behaviors.empty, "websocket")

  def main(args: Array[String]): Unit = {
    import system.executionContext

    val incoming: Sink[Message, Future[Done]] =
    Sink.foreach[Message] {
      case message: TextMessage.Strict =>
        println("received: " + message.text)
      case _ =>
      // ignore other message types
    }

    val (upgradeResponse: Future[WebSocketUpgradeResponse], closed: Future[Done]) = {
      val outgoing = Source.single(TextMessage("hello world!"))
      val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest("ws://echo.websocket.org"))
      outgoing.viaMat(webSocketFlow)(Keep.right)
              .toMat(incoming)(Keep.both)
              .run()
    }

    val connected: Future[Done.type] = upgradeResponse.flatMap { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        Future.successful(Done)
      } else {
        throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
      }
    }

    // in a real application you would not side effect here
    connected.onComplete(t=> println(s"Connected: " + t))
    closed.onComplete {
      case Success(_) =>
        println("closed")
        system.terminate()
      case Failure(ex) => throw ex
    }
  }
}
