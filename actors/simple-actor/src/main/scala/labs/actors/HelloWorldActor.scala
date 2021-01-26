package labs.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, LoggerOps}
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior

/**
 *
 * https://doc.akka.io/docs/akka/current/typed/actors.html
 *
 */

object HelloWorldMain {

  final case class SayHello(name: String)

  def apply(): Behavior[SayHello] = Behaviors.setup { ctx: ActorContext[SayHello] =>
    val gretter = ctx.spawn(HelloWorld(), "gretter")
    Behaviors.receiveMessage { message: SayHello =>
      val replyTo = ctx.spawn(HelloWorldBot(max = 3), message.name)
      gretter ! HelloWorld.Greet(message.name, replyTo)
      Behaviors.same
    }

  }

  val system: ActorSystem[SayHello] = ActorSystem(HelloWorldMain(), "hello")
  def main(args: Array[String]): Unit = {
    system ! SayHello("World")
    system ! SayHello("Akka")
  }

}


object HelloWorld {

  final case class Greet(whom: String, replyTo: ActorRef[Greeted])
  final case class Greeted(whom: String, from: ActorRef[Greet])

  def apply(): Behavior[Greet] = Behaviors.receive { (ctx: ActorContext[Greet], message: Greet) =>
    ctx.log.info("Hello {}!", message.whom)
    message.replyTo ! Greeted(message.whom, ctx.self)
    Behaviors.same
  }

}


object HelloWorldBot {

  def apply(max: Int): Behavior[HelloWorld.Greeted] = {
    bot(0, max)
  }

  private def bot(greetingCounter: Int, max: Int): Behavior[HelloWorld.Greeted] =
    Behaviors.receive { (ctx, message) =>
      val n = greetingCounter + 1
      ctx.log.info2(s"Greeting {} for {}", n, message.whom)
      if (n==max) {
        Behaviors.stopped
      } else {
        message.from ! HelloWorld.Greet(message.whom, ctx.self)
        bot(n, max)
      }
    }

}