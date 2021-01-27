package labs.actors

import akka.NotUsed
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets


/**
 *
 * Doc:
 *  https://doc.akka.io/docs/akka/current/typed/actors.html#a-more-complex-example
 *
 * Code:
 *  https://github.com/akka/akka/blob/ba3af3ea4614671fa0b27b9b0774ae08697b0acf/akka-actor-typed-tests/src/test/scala/docs/akka/typed/IntroSpec.scala#L132
 *
 */

object ChatRoomMain {

  def behavior(): Behavior[NotUsed] = Behaviors.setup { context =>
    val chatroomRef = context.spawn(ChatRoom(), "chatroom")
    val grabblerRef = context.spawn(GrabblerBehavior(), name = "grabbler")
    context.watch(grabblerRef)
    chatroomRef ! ChatRoom.GetSession("ol' Grabbler", grabblerRef)

    Behaviors.receiveSignal {
      case (_, Terminated(_)) =>
        Behaviors.stopped
    }
  }

  def main(args: Array[String]): Unit = {
    ActorSystem(ChatRoomMain.behavior(), "chatroom-main")
  }

}

object ChatRoom {
  sealed trait RoomCommand
  final case class GetSession(screenName: String, replyTo: ActorRef[SessionEvent]) extends RoomCommand

  sealed trait SessionEvent
  final case class SessionGranted(handle: ActorRef[PostMessage]) extends SessionEvent
  final case class SessionDenied(reason: String) extends SessionEvent
  final case class MessagePosted(screenName: String, message: String) extends SessionEvent

  sealed trait SessionCommand
  final case class PostMessage(message: String) extends SessionCommand

  def apply(): Behavior[RoomCommand] =
    ChatroomBehavior(List.empty)

  private final case class PublishSessionMessage(screenName: String, message: String) extends RoomCommand
  private final case class NotifyClient(message: MessagePosted) extends SessionCommand


  object ChatroomBehavior {
    def apply(sessions: List[ActorRef[SessionCommand]]): Behavior[RoomCommand] =
      Behaviors.receive { (context, message) =>
        context.log.info2("{}{}", this.getClass.getSimpleName, message)
        message match {
          case GetSession(screenName, clientRef) =>
            val sessionBehavior = SessionBehavior(context.self, screenName, clientRef)
            val sessionRef = context.spawn(sessionBehavior, URLEncoder.encode(screenName, StandardCharsets.UTF_8.name))
            clientRef ! SessionGranted(sessionRef)
            ChatroomBehavior(sessionRef :: sessions)

          case PublishSessionMessage(screenName, message) =>
            val notification = NotifyClient(MessagePosted(screenName, message))
            sessions.foreach(_ ! notification)
            Behaviors.same
        }
      }
  }


  object SessionBehavior {
    def apply(room: ActorRef[PublishSessionMessage],
            screenName: String,
            clientRef: ActorRef[SessionEvent]): Behavior[SessionCommand] =
      Behaviors.receive { (context, message) =>
        context.log.info2("{}{}", this.getClass.getSimpleName, message)
        message match {
          case PostMessage(message) =>
            room ! PublishSessionMessage(screenName, message)
            Behaviors.same
          case NotifyClient(message) =>
            clientRef ! message
            Behaviors.same
        }
      }
  }

}


object GrabblerBehavior {
  import ChatRoom._
  def apply(): Behavior[SessionEvent] = Behaviors.setup { context =>
    Behaviors.receive { (_, message) =>
      context.log.info2("{}{}", this.getClass.getSimpleName, message)
      message match {
        case SessionGranted(handle) =>
          handle ! PostMessage("Hello world!")
          Behaviors.same
        case MessagePosted(screenName, message) =>
          context.log.info2("message has been posted by '{}': {}", screenName, message)
          Behaviors.stopped
      }
    }

  }
}