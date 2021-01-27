package labs.http.docs



/**
 *
 * https://doc.akka.io/docs/akka-http/current/introduction.html#streaming
 *
 */
object HttpServerStreamingRandomNumbers extends SimpleBase {
  import akka.NotUsed
  import akka.actor.typed.ActorSystem
  import akka.actor.typed.scaladsl.Behaviors
  import akka.stream.scaladsl._
  import akka.http.scaladsl.Http
  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
  import akka.util.ByteString

  import scala.io.StdIn
  import scala.util.Random

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(Behaviors.empty, "RandomNumbers")
    implicit val executionContext = system.executionContext

    val numbers: Source[Int, NotUsed] =
      Source.fromIterator(() => Iterator.continually(Random.nextInt()))

    val route = path("random") {
      get {
        complete(
          HttpEntity(ContentTypes.`text/plain(UTF-8)`,
            numbers.map(n=>ByteString(s"$n\n"))
          )
        )
      }
    }

    val bindingFuture = Http().newServerAt(ALL_ADDRESSES, 8080).bind(route)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
            .flatMap(_.unbind())
            .onComplete(_ => system.terminate())

  }

}


/**
 *
 * https://doc.akka.io/docs/akka-http/current/introduction.html#streaming
 *
 */
object HttpServerWithActorInteraction extends SimpleBase {
  import akka.actor.typed.scaladsl.AskPattern._
  import akka.actor.typed.{ActorRef, ActorSystem}
  import akka.actor.typed.scaladsl.Behaviors
  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.Http
  import akka.http.scaladsl.model.StatusCodes
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  import akka.util.Timeout
  import spray.json.DefaultJsonProtocol._

  import scala.concurrent.{ExecutionContext, Future}
  import scala.concurrent.duration._
  import scala.io.StdIn

  object Auction {
    sealed trait Message
    case class Bid(userId: String, offer: Int) extends Message
    case class GetBids(replyTo: ActorRef[Bids]) extends Message
    case class Bids(bids: List[Bid]) extends Message
    def apply: Behaviors.Receive[Message] = apply(List.empty)

    def apply(bids: List[Bid]): Behaviors.Receive[Message] = Behaviors.receive {
      case (ctx, bid @ Bid(userId, offer)) =>
        ctx.log.info(s"Bid complete: $userId, $offer")
        apply(bids :+ bid)
      case (_, GetBids(replyTo)) =>
        replyTo ! Bids(bids)
        Behaviors.same
    }
  }

  implicit val bidFormat = jsonFormat2(Auction.Bid)
  implicit val bidsFormat = jsonFormat1(Auction.Bids)

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Auction.Message] = ActorSystem(Auction.apply, "auction")
    implicit val executionContext: ExecutionContext = system.executionContext

    val auction: ActorRef[Auction.Message] = system       // ActorSystemAdapter => ActorRef
    import Auction._

    val route = path("auction") {
      concat(
        put {
          parameters("bid".as[Int], "user") { (bid, user) =>
            auction ! Bid(user, bid)
            complete(StatusCodes.Accepted, "bid, placed")
          }
        },
        get {
          implicit val timeout: Timeout = 5.seconds
          val bids: Future[Bids] = (auction ? GetBids).mapTo[Bids]
          complete(bids)
        }
      )
    }

    val bindingFuture = Http().newServerAt(ALL_ADDRESSES, 8080).bind(route)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
            .flatMap(_.unbind())
            .onComplete(_ => system.terminate())

  }

}


/**
 *
 * https://doc.akka.io/docs/akka-http/current/introduction.html#low-level-http-server-apis
 *
 */
object HttpServerLowLevel extends SimpleBase {
  import akka.actor.typed.{ActorSystem}
  import akka.actor.typed.scaladsl.Behaviors
  import akka.http.scaladsl.Http
  import akka.http.scaladsl.model._
  import akka.http.scaladsl.model.HttpMethods._

  import scala.concurrent.{ExecutionContext}
  import scala.io.StdIn

  implicit val system = ActorSystem(Behaviors.empty, "lowlevel")
  implicit val executionContext: ExecutionContext = system.executionContext

  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
      HttpResponse(entity =
              HttpEntity(ContentTypes.`text/html(UTF-8)`, "<html><body>Hello world!</body></html>")
      )
    case HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
      HttpResponse(entity = "PONG!")
    case HttpRequest(GET, Uri.Path("/crash"), _, _, _) =>
      sys.error("BOOM")
    case r: HttpRequest =>
      r.discardEntityBytes()
      HttpResponse(404, entity = "Unknown resource!")
  }

  def main(args: Array[String]): Unit = {
    val bindingFuture = Http().newServerAt(ALL_ADDRESSES, 8080).bindSync(requestHandler)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
            .flatMap(_.unbind())
            .onComplete(_ => system.terminate())
  }

}


/**
 * https://doc.akka.io/docs/akka-http/current/introduction.html#low-level-http-server-apis
 */
object HttpClientSingleRequest {

  import akka.actor.typed.ActorSystem
  import akka.actor.typed.scaladsl.Behaviors
  import akka.http.scaladsl.Http
  import akka.http.scaladsl.model._

  import scala.concurrent.Future
  import scala.util.{ Failure, Success }

  implicit val system = ActorSystem(Behaviors.empty, "SingleRequest")
  implicit val executionContext = system.executionContext

  def main(args: Array[String]): Unit = {

    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = "http://akka.io"))

    responseFuture
            .onComplete {
              case Success(res) => println(res)
              case Failure(_) => sys.error("something wrong")
            }

  }

}