package ch.epfl.ts.benchmark

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import ch.epfl.ts.ch.epfl.ts.data.{Currency, Transaction}

import scala.concurrent.duration.FiniteDuration
import scala.io.Source

abstract class TimedReporterActor(master: ActorRef, dest: List[ActorRef], source: String) extends Actor {
  val system = this.context.system
  import system.dispatcher

  def receive = {
    case c: Start => this.context.system.scheduler.scheduleOnce(FiniteDuration(c.offset, scala.concurrent.duration.MILLISECONDS))(
      self ! RunItNow
      //readAndSend
    )
    case RunItNow => readAndSend
  }

  case class RunItNow()

  def readAndSend: Unit = {
    val startTime = System.currentTimeMillis
    f()
    val endTime = System.currentTimeMillis
    master ! Report(source, startTime, endTime)
  }

  def f(): Unit
}

object TimedReporterActor {
  def fileFetchActor(sys: ActorSystem, master: ActorRef, filename: String): (List[ActorRef] => ActorRef) = {
    (dest: List[ActorRef]) => sys.actorOf(Props(classOf[FileFetch], master, filename, dest))
  }
}

class FileFetch(master: ActorRef, filename: String, dest: List[ActorRef]) extends TimedReporterActor(master, dest, "Generator") {
  override def f(): Unit = Source.fromFile(filename).getLines().foreach(
    s => {
      val l = s.split(",")
//      dest.map(_ ! Transaction(l(1).toDouble, l(2).toDouble, l(0).toLong, Currency.withName(l(3).toLowerCase), l(4), l(5)))
    }
  )
}