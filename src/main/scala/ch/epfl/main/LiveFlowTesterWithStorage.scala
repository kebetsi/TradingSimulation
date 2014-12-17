package ch.epfl.main

import akka.actor._
import ch.epfl.ts.component.{Component, ComponentBuilder}
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.first.PersistanceComponent
import ch.epfl.ts.first.fetcher.BtceTransactionPullFetcherComponent
import ch.epfl.ts.impl.TransactionPersistorImpl

object LiveFlowTesterWithStorage {


  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("DataSourceSystem")
    implicit val builder = new ComponentBuilder()

    val printer = Props(new Printer("my-printer"))
    val persistor = Props(new PersistanceComponent[Transaction](new TransactionPersistorImpl()))
    val fetcher = Props(new BtceTransactionPullFetcherComponent("my-fetcher"))
    
    //fetcher.addDestination(printer, classOf[Transaction])
    //fetcher.addDestination(persistor, classOf[Transaction])

    builder.start
  }
}

// Stage 2, used just to print out the result from stage 1
class Printer(val name: String) extends Component {
  def receiver = {
    case t: Transaction => println(System.currentTimeMillis, t.toString)
  }
}

