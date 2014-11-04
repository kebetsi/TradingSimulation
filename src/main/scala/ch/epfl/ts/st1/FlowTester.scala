package ch.epfl.ts.st1

import ch.epfl.ts.data.Transaction
import ch.epfl.ts.first._
import akka.actor.{ ActorSystem, Props, Actor}
import ch.epfl.ts.impl.PullFetchTransactionImpl
import ch.epfl.ts.impl.TransactionPersistorImpl

object FlowTester {
  
  
  class Printer extends Actor {
    override def receive = {
      case t: Transaction => println(System.currentTimeMillis, t.toString)
      case _  => 
    }
  }
  
  def main(args: Array[String]) = {
    val system = ActorSystem("DataSourceSystem")
  
 
    val dest = system.actorOf(Props(classOf[Printer]), "instage-printer")
    val is = system.actorOf(Props(classOf[InStage[Transaction]], 
        List(dest), new PullFetchTransactionImpl(), new TransactionPersistorImpl() ), 
        "instage-inst")
  }

}