package ch.epfl.ts.benchmark.scala

import akka.actor.Actor
import ch.epfl.ts.data.{Order, Transaction}

case class Start(offset: Long)

case object Stop

case class Report(source: String, startTime: Long, endTime: Long)

class Reporter extends Actor {
  var genStart: Long = 0
  var conEnd: Long = 0
  override def receive = {
    case r: Report => {
      println(r.source, "time", r.endTime - r.startTime)
      if (r.source == "Generator") {
        genStart = r.startTime
      } else if (r.source == "Consumer") {
        conEnd = r.endTime
      }

      if (genStart != 0 && conEnd != 0) {println("Total time", conEnd - genStart)}
    }
  }
}

class Printer extends Actor {
  override def receive = {
    case t: Transaction => println(System.currentTimeMillis, "Trans", t.toString)
    case o: Order => println(System.currentTimeMillis, "Order", o.toString)
  }
}