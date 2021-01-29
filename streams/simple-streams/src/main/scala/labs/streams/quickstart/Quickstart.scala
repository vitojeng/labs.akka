package labs.streams.quickstart

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Keep, Sink, Source, Flow}
import akka.util.ByteString

import java.io.File
import java.nio.file.Paths
import scala.concurrent._
import scala.concurrent.duration._

/**
 *
 * https://doc.akka.io/docs/akka/current/stream/stream-quickstart.html
 *
 */

object Quickstart1 {
  implicit val system = ActorSystem("quickstart1")

  def main(args: Array[String]): Unit = {
    val source: Source[Int, NotUsed] = Source(1 to 100)
    val done: Future[Done] = source.runForeach(i => println(i))

    implicit val ec = system.dispatcher
    done.onComplete(_ => system.terminate())
  }
}

object Qucikstart2 {
  implicit val system = ActorSystem("quickstart2")

  def main(args: Array[String]): Unit = {
    val source: Source[Int, NotUsed] = Source(1 to 100)

    val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

    new File("factorials.txt").delete()
    new File("factorial2.txt").delete()

    val wt = 1
    val result: Future[IOResult] = wt match {
        case 0 =>
          factorials.map(num => ByteString(s"$num\n"))
                  .runWith(FileIO.toPath(Paths.get("factorials.txt")))
        case 1 =>
          factorials.map(_.toString).runWith(lineSink("factorial2.txt"))
      }

    implicit val ec = system.dispatcher
    result.onComplete(_ => system.terminate())
  }

  def lineSink(filename: String): Sink[String, Future[IOResult]] = {
    Flow[String].map(s => ByteString(s+"\n"))
            .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)
  }

}

object Quickstart3 {
  implicit val system = ActorSystem("quickstart3")

  def main(args: Array[String]): Unit = {
    val source: Source[Int, NotUsed] = Source(1 to 100)

    val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

    val result: Future[Done] =
      factorials
              .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
              .throttle(1, 100.millisecond)
              .take(10)
              .runWith(Sink.foreach(println))

    implicit val ec = system.dispatcher
    result.onComplete(_ => system.terminate())

  }
}
