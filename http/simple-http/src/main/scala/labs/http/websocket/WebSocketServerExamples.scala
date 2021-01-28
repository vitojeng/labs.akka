package labs.http.websocket

import labs.http.SimpleBase

/**
 *
 * https://doc.akka.io/docs/akka-http/10.2.3/server-side/websocket-support.html#websocketupgrade
 *
 * https://github.com/akka/akka-http/blob/afc81cb9205b18446745ae12f8e6a575d98f6b18/docs/src/test/scala/docs/http/scaladsl/server/WebSocketExampleSpec.scala#L23
 *
 * Test:
 * $ websocat ws://localhost:8080/gretter
 *
 */
object WebSocketCoreExample extends SimpleBase {
  import akka.actor.typed.ActorSystem
  import akka.actor.typed.scaladsl.Behaviors
  import akka.http.scaladsl.Http
  import akka.http.scaladsl.model.ws.{Message, BinaryMessage, TextMessage}
  import akka.http.scaladsl.model.{HttpRequest, HttpResponse, AttributeKeys, Uri}
  import akka.http.scaladsl.model.HttpMethods._
  import akka.stream.scaladsl.{Sink, Source, Flow}

  import scala.io.StdIn


  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "websocket")

  private val gretterHandlerFlow =
    Flow[Message].mapConcat {
      case tm: TextMessage =>
        TextMessage(Source.single("Hello ") ++ tm.textStream) :: Nil
      case bm: BinaryMessage =>
        bm.dataStream.runWith(Sink.ignore)
        Nil
  }

  val requestHandler: HttpRequest => HttpResponse = {
    case req @ HttpRequest(GET, Uri.Path("/gretter"), _, _, _) =>
      req.attribute(AttributeKeys.webSocketUpgrade) match {
        case Some(upgrade) =>
          upgrade.handleMessages(gretterHandlerFlow)
        case None =>
          HttpResponse(400, entity = "Not a valid websocket request!")
      }
    case r: HttpRequest =>
      r.discardEntityBytes()
      HttpResponse(404, entity = "Unknown resource!")
  }

  def main(args: Array[String]): Unit = {
    val bindingFuture = Http().newServerAt(ALL_ADDRESSES, 8080).bindSync(requestHandler)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()

    import system.executionContext
    bindingFuture.flatMap(_.unbind())
            .onComplete(_ => system.terminate())
  }

}


/**
 *
 * https://doc.akka.io/docs/akka-http/10.2.3/server-side/websocket-support.html#routing-support
 *
 * https://github.com/akka/akka-http/blob/afc81cb9205b18446745ae12f8e6a575d98f6b18/docs/src/test/scala/docs/http/scaladsl/server/WebSocketExampleSpec.scala#L23
 *
 * Test:
 * $ websocat ws://localhost:8080/gretter
 *
 */
object WebSocketRoutingExample extends SimpleBase {
  import akka.actor.typed.ActorSystem
  import akka.actor.typed.scaladsl.Behaviors
  import akka.http.scaladsl.Http
  import akka.http.scaladsl.model.ws.{Message, TextMessage}
  import akka.http.scaladsl.server.Directives._
  import akka.stream.scaladsl.{Source, Flow}
  import akka.util.ByteString

  import scala.io.StdIn
  import java.util.concurrent.atomic.AtomicInteger




  implicit val system = ActorSystem(Behaviors.empty, "websocket")

  private val gretterHandlerFlow =
    Flow[Message].collect {
      case tm: TextMessage =>
        TextMessage(Source.single("Hello ") ++ tm.textStream)
  }

  val route = path("gretter") {
    get {
      handleWebSocketMessages(gretterHandlerFlow)
    }
  }

  val pingCounter = new AtomicInteger()

  def main(args: Array[String]): Unit = {
    val bindingFuture = Http().newServerAt(ALL_ADDRESSES, 8080)
            .adaptSettings(_.mapWebsocketSettings(
              _.withPeriodicKeepAliveData(()=>ByteString(s"debug-${pingCounter.incrementAndGet()}"))
            ))
            .bind(route)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()

    import system.executionContext
    bindingFuture.flatMap(_.unbind())
            .onComplete(_ => system.terminate())
  }

}


object WebSocketPingServerExample extends SimpleBase {
  import akka.actor.typed.ActorSystem
  import akka.actor.typed.scaladsl.Behaviors
  import akka.http.scaladsl.Http
  import akka.http.scaladsl.settings.ServerSettings
  import akka.util.ByteString

  import java.util.concurrent.atomic.AtomicInteger

  implicit val system = ActorSystem(Behaviors.empty, "websocket")

  def main(args: Array[String]): Unit = {
    val route = null

    val defaultSettings = ServerSettings(system)
    val pingCounter = new AtomicInteger()

    Http().newServerAt(ALL_ADDRESSES, 8080)
            .adaptSettings(_.mapWebsocketSettings(
              _.withPeriodicKeepAliveData(()=>ByteString(s"debug-${pingCounter.incrementAndGet()}"))
            ))
            .bind(route)


  }
}