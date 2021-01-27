package labs.http.chat

import akka.actor.typed.scaladsl.Behaviors

object ChatServer {
  import akka.actor.ActorSystem
  import akka.http.scaladsl.Http
  import akka.stream.Materializer
  import akka.http.scaladsl.server.Directives._

  import java.util.concurrent.Executors
  import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}


  implicit val system: ActorSystem = ActorSystem("app")
  implicit val materializer: Materializer = Materializer.matFromSystem
  implicit val executionContext: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(16))

  private val chatroom = new ChatRoom()

  def main(args: Array[String]): Unit = {
    val webRoute = pathEndOrSingleSlash {
      getFromResource("web/index.html")
    }

    val apiRoute = path("api" / "chat") {
      get {
        parameters("name") { name =>
          handleWebSocketMessages(chatroom.websocketFlow(name))
        }
      }
    }

    Http().bindAndHandle(apiRoute ~ webRoute, "0.0.0.0", 8080)
            .map { _ =>
              println(s"Server is running at http://localhost:8080/")
            }

  }

}

object ChatServer2 {
  import akka.actor.typed.ActorSystem

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "chatservers")
  implicit

  def main(args: Array[String]): Unit = {

  }

}
