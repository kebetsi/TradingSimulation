package ch.epfl.main

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.fetch.{ BtceTransactionPullFetcher, PullFetchComponent, BitstampTransactionPullFetcher }
import ch.epfl.ts.component.persist.{ Persistor, TransactionPersistor }
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.data.Transaction
import scala.reflect.ClassTag
import ch.epfl.ts.traders.Arbitrageur
import ch.epfl.ts.engine.MarketSimulator
import ch.epfl.ts.engine.MarketRules
import ch.epfl.ts.component.fetch.MarketNames

object BTCArbitrage {

  def main(args: Array[String]) {
    implicit val builder = new ComponentBuilder("ArbitrageSystem")

    // Initialize the Interface to DB
    val btceXactPersit = new TransactionPersistor("btce-transaction-db2")
    btceXactPersit.init()
    val bitstampXactPersist = new TransactionPersistor("bitstamp-transaction-db2")
    bitstampXactPersist.init()

    // Instantiate Transaction fetchers Bitcoin exchange markets
    val btceMarketId = MarketNames.BTCE_ID
    val bitstampMarketId = MarketNames.BITSTAMP_ID
    val btcePullFetcher = new BtceTransactionPullFetcher
    val bitstampPullFetcher = new BitstampTransactionPullFetcher

    // Create Components
    // markets
    val rules = new MarketRules()
    val btceMarket = builder.createRef(Props(classOf[MarketSimulator], btceMarketId, rules))
    val bitstampMarket = builder.createRef(Props(classOf[MarketSimulator], bitstampMarketId, rules))
    val printer = builder.createRef(Props(classOf[Printer], "my-printer"))
    val btcePersistor = builder.createRef(Props(classOf[Persistor[Transaction]], btceXactPersit, implicitly[ClassTag[Transaction]]))
    val bitstampPersistor = builder.createRef(Props(classOf[Persistor[Transaction]], bitstampXactPersist, implicitly[ClassTag[Transaction]]))
    val btceFetcher = builder.createRef(Props(classOf[PullFetchComponent[Transaction]], btcePullFetcher, implicitly[ClassTag[Transaction]]))
    val bitstampFetcher = builder.createRef(Props(classOf[PullFetchComponent[Transaction]], bitstampPullFetcher, implicitly[ClassTag[Transaction]]))
    val arbitrageur = builder.createRef(Props(classOf[Arbitrageur], 111L, btceMarketId, bitstampMarketId))

    // Create the connections
    //    btceFetcher.addDestination(printer, classOf[Transaction])
    //    btceFetcher.addDestination(btcePersistor, classOf[Transaction])
    btceFetcher.addDestination(arbitrageur, classOf[Transaction])
    //    bitstampFetcher.addDestination(printer, classOf[Transaction])
    bitstampFetcher.addDestination(arbitrageur, classOf[Transaction])
    //    bitstampFetcher.addDestination(bitstampPersistor, classOf[Transaction])

    // Start the system
    builder.start
  }
}