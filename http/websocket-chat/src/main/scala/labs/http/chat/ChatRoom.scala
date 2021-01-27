package labs.http.chat

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import ChatMessages.{UserJoined, UserLeft, UserSaid}
import org.reactivestreams.Publisher

import scala.collection.mutable

class ChatRoom()(implicit system: ActorSystem, mat: Materializer) {
  private val roomActor = system.actorOf(Props(classOf[ChatRoomActor]))

  def websocketFlow(name: String): Flow[Message, Message, Any] = {
    val (actorRef: ActorRef, publisher: Publisher[TextMessage.Strict]) =
      Source.actorRef[String](16, OverflowStrategy.fail)
              .map(msg => TextMessage.Strict(msg))
              .toMat(Sink.asPublisher(false))(Keep.both).run()

    roomActor ! UserJoined(name, actorRef)

    val sink = Flow[Message]
              .map {
                case TextMessage.Strict(msg) => roomActor ! UserSaid(name, msg)
              }
              .to(
                Sink.onComplete( _ => roomActor ! UserLeft(name))
              )

    Flow.fromSinkAndSource(sink, Source.fromPublisher(publisher))
  }

}

class ChatRoomActor() extends Actor {
  val users: mutable.Map[String, ActorRef] = mutable.Map.empty[String, ActorRef]

  override def receive: Receive = {
    case UserJoined(name, userActor) =>
      users.put(name, userActor)
      println(s"$name joined the chatroom.")
      broadcast(s"$name joined the chatroom")

    case UserLeft(name) =>
      users.remove(name)
      println(s"$name left the chatroom.")
      broadcast(s"$name left the chatroom")

    case UserSaid(name, msg) =>
      println(s"$name: $msg")
      broadcast(s"$name: $msg")
  }

  def broadcast(msg: String): Unit = {
    users.values.foreach(_ ! msg)
  }
}
