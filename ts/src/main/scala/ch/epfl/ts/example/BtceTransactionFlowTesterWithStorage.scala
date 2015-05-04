package ch.epfl.ts.example

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.fetch.{BitstampTransactionPullFetcher, BtceTransactionPullFetcher, MarketNames, PullFetchComponent}
import ch.epfl.ts.component.persist.{Persistor, TransactionPersistor}
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.data.Transaction

import scala.reflect.ClassTag

/**
 * Demonstration of fetching Live Bitcoin/USD trading data from BTC-e,
 * saving it to a SQLite Database and printing it.
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
    val btceMarketId = MarketNames.BTCE_ID
    val bitstampMarketId = MarketNames.BITSTAMP_ID
    val btcePullFetcher = new BtceTransactionPullFetcher
    val bitstampPullFetcher = new BitstampTransactionPullFetcher

    // Create Components
    val printer = builder.createRef(Props(classOf[Printer], "my-printer"), "printer")
    val btcePersistor = builder.createRef(Props(classOf[Persistor[Transaction]], btceXactPersit, implicitly[ClassTag[Transaction]]), "btcePersistor")
    val btceFetcher = builder.createRef(Props(classOf[PullFetchComponent[Transaction]], btcePullFetcher, implicitly[ClassTag[Transaction]]), "btceFetcher")
    val bitstampPersistor = builder.createRef(Props(classOf[Persistor[Transaction]], bitstampXactPersit, implicitly[ClassTag[Transaction]]), "bitstampPeristor")
    val bitstampFetcher = builder.createRef(Props(classOf[PullFetchComponent[Transaction]], bitstampPullFetcher, implicitly[ClassTag[Transaction]]), "bitstampFetcher")

    // Create the connections
    btceFetcher->(Seq(printer, btcePersistor), classOf[Transaction])
    bitstampFetcher->(Seq(printer, bitstampPersistor), classOf[Transaction])

    // Start the system
    builder.start
  }
}
