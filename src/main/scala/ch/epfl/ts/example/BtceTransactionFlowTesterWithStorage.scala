package ch.epfl.ts.example

import akka.actor.Props
import ch.epfl.ts.component.fetch.BtceTransactionPullFetcherComponent
import ch.epfl.ts.component.persist.TransactionPersistanceComponent
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.component.{Component, ComponentBuilder}
import ch.epfl.ts.data.Transaction

/**
 * Demonstration of fetching Live Bitcoin/USD trading data from BTC-e,
 * saving it to a SQLite Database and printing it on the other side.
 */
object BtceTransactionFlowTesterWithStorage {
  def main(args: Array[String]): Unit = {
    implicit val builder = new ComponentBuilder("DataSourceSystem")

    val printer = builder.createRef(Props(classOf[Printer], "my-printer"))
    val persistor = builder.createRef(Props(classOf[TransactionPersistanceComponent], "btce-transaction-db"))
    val fetcher = builder.createRef(Props(classOf[BtceTransactionPullFetcherComponent], "my-fetcher"))

    fetcher.addDestination(printer, classOf[Transaction])
    fetcher.addDestination(persistor, classOf[Transaction])

    builder.start
  }
}


