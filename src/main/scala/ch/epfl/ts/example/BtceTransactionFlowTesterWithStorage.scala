package ch.epfl.ts.example

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.fetch.{ BtceTransactionPullFetcher, PullFetchComponent, BitstampTransactionPullFetcher }
import ch.epfl.ts.component.persist.{ Persistor, TransactionPersistor }
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
    val btceXactPersit = new TransactionPersistor("btce-transaction-db-batch")
    btceXactPersit.init()
    val bitstampXactPersit = new TransactionPersistor("bitstamp-transaction-db-batch")
    bitstampXactPersit.init()

    // Instantiate a Transaction etcher for BTC-e and Bitstamp
    val btceMarketId= 1
    val bitstampMarketId= 2
    val btcePullFetcher = new BtceTransactionPullFetcher(btceMarketId)
    val bitstampPullFetcher = new BitstampTransactionPullFetcher(bitstampMarketId)

    // Create Components
    val printer = builder.createRef(Props(classOf[Printer], "my-printer"))
    val btcePersistor = builder.createRef(Props(classOf[Persistor[Transaction]], btceXactPersit, implicitly[ClassTag[Transaction]]))
    val btceFetcher = builder.createRef(Props(classOf[PullFetchComponent[Transaction]], btcePullFetcher, implicitly[ClassTag[Transaction]]))
    val bitstampPersistor = builder.createRef(Props(classOf[Persistor[Transaction]], bitstampXactPersit, implicitly[ClassTag[Transaction]]))
    val bitstampFetcher = builder.createRef(Props(classOf[PullFetchComponent[Transaction]], bitstampPullFetcher, implicitly[ClassTag[Transaction]]))

    // Create the connections
    btceFetcher.addDestination(printer, classOf[Transaction])
    btceFetcher.addDestination(btcePersistor, classOf[Transaction])
    bitstampFetcher.addDestination(printer, classOf[Transaction])
    bitstampFetcher.addDestination(bitstampPersistor, classOf[Transaction])

    // Start the system
    builder.start
  }
}
