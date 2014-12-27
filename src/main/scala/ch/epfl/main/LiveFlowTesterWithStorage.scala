package ch.epfl.main

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.first.{Persistance, TransactionPersistanceComponent}
import ch.epfl.ts.first.fetcher.BtceTransactionPullFetcherComponent
import ch.epfl.ts.impl.TransactionPersistorImpl

object LiveFlowTesterWithStorage {


  def main(args: Array[String]): Unit = {
    implicit val builder = new ComponentBuilder("DataSourceSystem")

    val transacPerst: Persistance[Transaction] = new TransactionPersistorImpl()


    val printer = builder.createRef(Props(classOf[Printer], "my-printer"))
    val persistor = builder.createRef(Props(classOf[TransactionPersistanceComponent], transacPerst))
    val fetcher = builder.createRef(Props(classOf[BtceTransactionPullFetcherComponent], "my-fetcher"))

    fetcher.addDestination(printer, classOf[Transaction])
    fetcher.addDestination(persistor, classOf[Transaction])

    builder.start
  }
}

// Stage 2, used just to print out the result from stage 1
class Printer(val name: String) extends Component {
  def receiver = {
    case t: Transaction => println(System.currentTimeMillis, t.toString)
    case x => println("Printer got: " + x.getClass.toString)
  }
}

