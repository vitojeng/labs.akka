package labs.http.chat

import akka.actor.ActorRef

object ChatMessages {

  sealed trait UserEvent

  case class UserJoined(name: String, userActor: ActorRef) extends UserEvent {
    override def equals(obj: Any): Boolean = this.asInstanceOf[UserJoined].equals(obj)
  }

  case class UserLeft(name: String) extends UserEvent {
    override def equals(obj: Any): Boolean = this.asInstanceOf[UserLeft].equals(obj)
  }

  case class UserSaid(name: String, message: String) extends UserEvent {
    override def equals(obj: Any): Boolean = this.asInstanceOf[UserSaid].equals(obj)
  }

}
