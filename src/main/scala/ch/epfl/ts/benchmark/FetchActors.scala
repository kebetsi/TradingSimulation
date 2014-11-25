package ch.epfl.ts.benchmark

import akka.actor.{Actor, ActorRef}
import ch.epfl.ts.data.{Currency, Transaction}

import scala.concurrent.duration.FiniteDuration
import scala.io.Source

abstract class TimedReporterActor(master: ActorRef, dest: ActorRef) extends Actor {
  def receive = {
    case c: Run => this.context.system.scheduler.scheduleOnce(FiniteDuration(c.offset, scala.concurrent.duration.MILLISECONDS)) {
      readAndSend
    }
  }
  def readAndSend: Unit = {
    val startTime = System.currentTimeMillis
    f()
    val endTime = System.currentTimeMillis
    master ! Report(startTime, endTime)
  }
  def f(): Unit
}

class FileFetchActor(master: ActorRef, destination: ActorRef, filename: String)
  extends TimedReporterActor(master, destination) {
  override def f = Source.fromFile(filename).getLines().foreach(
    s => {
      val l = s.split(",")
      destination ! Transaction(l(1).toDouble, l(2).toDouble, l(0).toLong, Currency.withName(l(3).toLowerCase), l(4), l(5))
    }
  )
}
