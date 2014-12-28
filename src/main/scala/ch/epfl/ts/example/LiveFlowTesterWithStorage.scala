package ch.epfl.ts.example

import akka.actor.Props
import ch.epfl.ts.component.fetch.BtceTransactionPullFetcherComponent
import ch.epfl.ts.component.{Component, ComponentBuilder}
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.first.{Persistance, TransactionPersistanceComponent}
import ch.epfl.ts.impl.TransactionPersistorImpl

/**
 * Demonstration of fetching Live Bitcoin/USD trading data from BTC-e,
 * saving it to a SQLite Database and printing it on the other side.
 */
object LiveFlowTesterWithStorage {
  def main(args: Array[String]): Unit = {
    implicit val builder = new ComponentBuilder("DataSourceSystem")

    val transacPerst: Persistance[Transaction] = new TransactionPersistorImpl("FlowTester")

    val printer = builder.createRef(Props(classOf[Printer], "my-printer"))
    val persistor = builder.createRef(Props(classOf[TransactionPersistanceComponent], transacPerst))
    val fetcher = builder.createRef(Props(classOf[BtceTransactionPullFetcherComponent], "my-fetcher"))

    fetcher.addDestination(printer, classOf[Transaction])
    fetcher.addDestination(persistor, classOf[Transaction])

    builder.start
  }
}

/**
 * Simple printer component for Transactions.
 * @param name The name of the component.
 */
class Printer(val name: String) extends Component {
  def receiver = {
    case t: Transaction => println(System.currentTimeMillis, t.toString)
    case _ =>
  }
}
