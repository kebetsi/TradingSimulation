package ch.epfl.ts.benchmark

import akka.actor.Actor
import ch.epfl.ts.data.Transaction

case class Start(offset: Long)

case class Stop()

case class Report(source: String, startTime: Long, endTime: Long)

class Reporter extends Actor {
  override def receive = {
    case r: Report => println(r.source, "time", r.endTime - r.startTime)
  }
}

class Printer extends Actor {
  override def receive = {
    case t: Transaction => println(System.currentTimeMillis, "Trans", t.toString)
    case o: Ordered => println(System.currentTimeMillis, "Order", o.toString)
  }
}