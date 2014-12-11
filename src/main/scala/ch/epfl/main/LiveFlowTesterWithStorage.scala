package ch.epfl.main

import akka.actor._
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.first.InStage
import ch.epfl.ts.first.fetcher.BtceTransactionPullFetcher
import ch.epfl.ts.impl.TransactionPersistorImpl

object LiveFlowTesterWithStorage {
  // Stage 2, used just to print out the result from stage 1
  class Printer extends Actor {
    override def receive = {
      case t: Transaction => println(System.currentTimeMillis, t.toString)
      case _  => 
    }
  }

  def main(args: Array[String]) = {
    val system = ActorSystem("DataSourceSystem")
    val printer = system.actorOf(Props(classOf[Printer]), "instage-printer")
    val persistor = new TransactionPersistorImpl()
    persistor.init()
    val instage = (new InStage[Transaction](system, List(printer)))
      .withPersistance(persistor)
      .withFetchInterface(new BtceTransactionPullFetcher()).start
  }
}