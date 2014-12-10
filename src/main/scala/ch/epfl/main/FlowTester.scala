

package ch.epfl.main

import ch.epfl.ts.data.Transaction
import ch.epfl.ts.first.InStage
import akka.actor._
import ch.epfl.ts.first.fetcher.BtceTransactionPullFetcher
import ch.epfl.ts.impl.TransactionPersistorImpl

object FlowTester {
  
  
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

    val instage = (new InStage[Transaction](system, List(printer)))
      .withPersistance(new TransactionPersistorImpl())
      .withFetchInterface(new BtceTransactionPullFetcher()).start

    /*
    val is = system.actorOf(Props(classOf[InStage[Transaction]], List(printer), 
        , new TransactionPersistorImpl() ), "instage-inst")
    is ! "init"
    */
  }

}