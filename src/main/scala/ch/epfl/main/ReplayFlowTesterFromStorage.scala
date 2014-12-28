package ch.epfl.main

import akka.actor._
import ch.epfl.ts.benchmark.Start
import ch.epfl.ts.component.fetch.BtceTransactionPullFetcher
import ch.epfl.ts.component.persist.TransactionPersistor
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.first.InStage

object ReplayFlowTesterFromStorage {
  // Stage 2, used just to print out the result from stage 1
  class Printer extends Actor {
    override def receive = {
      case t: Transaction => println(System.currentTimeMillis, t.toString)
      case _  => 
    }
  }

  /*def main(args: Array[String]) = {
    val system = ActorSystem("DataSourceSystem")
    val printer = system.actorOf(Props(classOf[Printer]), "instage-printer")
    val persistor = new TransactionPersistorImpl("ReplayFlowTesterFromStorage")
    persistor.init()
    val instage = new InStage[Transaction](system, List(printer))
      .withPersistance(persistor)
      .withReplay(1418737788400L,0.01).start
    instage ! "Start"
  }*/
}