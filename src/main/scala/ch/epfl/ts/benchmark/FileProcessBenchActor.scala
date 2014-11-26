package ch.epfl.ts.benchmark

import akka.actor.{ActorSystem, Props}
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.first.InStage

object FileProcessBenchActor {
  def main(args: Array[String]) = {
    val system = ActorSystem("DataSourceSystem")
    val printer = system.actorOf(Props[Printer])
    val reporter = system.actorOf(Props[Reporter])

    val mainActor = new InStage[Transaction](system, List(printer))
      .withFetcherActor(TimedReporterActor.fileFetchActor(system, reporter, "asdf"))
      .start

    mainActor ! new Start(0)
  }
}
