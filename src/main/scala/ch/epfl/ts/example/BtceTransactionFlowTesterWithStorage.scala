package ch.epfl.ts.example

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.fetch.{BtceTransactionPullFetcher, PullFetchComponent}
import ch.epfl.ts.component.persist.{Persistor, TransactionPersistor}
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.data.Transaction

import scala.reflect.ClassTag

/**
 * Demonstration of fetching Live Bitcoin/USD trading data from BTC-e,
 * saving it to a SQLite Database and printing it on the other side.
 */
object BtceTransactionFlowTesterWithStorage {
  def main(args: Array[String]): Unit = {
    implicit val builder = new ComponentBuilder("DataSourceSystem")

    // Initialize the Interface to DB
    val btceXactPersit = new TransactionPersistor("btce-transaction-db")
    btceXactPersit.init()

    // Instantiate a Transaction etcher for BTC-e
    val btcePullFetcher = new BtceTransactionPullFetcher()

    // Create Components
    val printer = builder.createRef(Props(classOf[Printer], "my-printer"))
    val persistor = builder.createRef(Props(classOf[Persistor[Transaction]], btceXactPersit, implicitly[ClassTag[Transaction]]))
    val fetcher = builder.createRef(Props(classOf[PullFetchComponent[Transaction]], btcePullFetcher, implicitly[ClassTag[Transaction]]))

    // Create the connections
    fetcher.addDestination(printer, classOf[Transaction])
    fetcher.addDestination(persistor, classOf[Transaction])

    // Start the system
    builder.start
  }
}
