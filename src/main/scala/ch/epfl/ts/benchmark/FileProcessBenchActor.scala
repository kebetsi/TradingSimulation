package ch.epfl.ts.benchmark

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import ch.epfl.ts.ch.epfl.ts.data.Transaction
import ch.epfl.ts.ch.epfl.ts.first.InStage

object FileProcessBenchActor {
  def main(args: Array[String]) = {
    val system = ActorSystem("DataSourceSystem")
    val reporter = system.actorOf(Props[Reporter])
    val printer = system.actorOf(Props(classOf[ConsumerActor], reporter))

    val mainActor = new InStage[Transaction](system, List(printer))
      .withFetcherActor(TimedReporterActor.fileFetchActor(system, reporter, "fakeData.csv"))
      .start

    mainActor ! new Start(0)
  }
}

class ConsumerActor(reporter: ActorRef) extends Actor {
  var notInit = true
  var startTime: Long = 0
  var count: Long = 0
  override def receive = {
    case t:Transaction => timeConsume
  }
  def timeConsume: Unit = {
    if (notInit) {
      notInit = false
      startTime = System.currentTimeMillis()
    }
    count += 1
    //println(count)
    if (count == 999477) {
      val endTime = System.currentTimeMillis()
      reporter ! Report("Consumer", startTime, endTime)
    }
  }
}

