package ch.epfl.main

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.fetch.{ BtceTransactionPullFetcher, PullFetchComponent, BitstampTransactionPullFetcher }
import ch.epfl.ts.component.persist.{ Persistor, TransactionPersistor }
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.data.{ Order, Transaction, LimitOrder, LiveLimitAskOrder, LiveLimitBidOrder, DelOrder }
import scala.reflect.ClassTag
import ch.epfl.ts.traders.Arbitrageur
import ch.epfl.ts.engine.MarketSimulator
import ch.epfl.ts.engine.MarketRules
import ch.epfl.ts.component.fetch.MarketNames
import ch.epfl.ts.component.fetch.BtceOrderPullFetcher
import ch.epfl.ts.component.fetch.PullFetchListComponent
import ch.epfl.ts.component.fetch.BitstampOrderPullFetcher

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
    val btceTransactionPullFetcher = new BtceTransactionPullFetcher
    val btceOrderPullFetcher = new BtceOrderPullFetcher
    val bitstampTransactionPullFetcher = new BitstampTransactionPullFetcher
    val bitstampOrderPullFetcher = new BitstampOrderPullFetcher

    // Create Components
    // markets
    val rules = new MarketRules()
    val btceMarket = builder.createRef(Props(classOf[MarketSimulator], btceMarketId, rules))
    val bitstampMarket = builder.createRef(Props(classOf[MarketSimulator], bitstampMarketId, rules))
    // printer
    val printer = builder.createRef(Props(classOf[Printer], "my-printer"))
    // persistors
    val btcePersistor = builder.createRef(Props(classOf[Persistor[Transaction]], btceXactPersit, implicitly[ClassTag[Transaction]]))
    val bitstampPersistor = builder.createRef(Props(classOf[Persistor[Transaction]], bitstampXactPersist, implicitly[ClassTag[Transaction]]))
    // fetchers
    //    val btceFetcher = builder.createRef(Props(classOf[PullFetchComponent[Transaction]], btceTransactionPullFetcher, implicitly[ClassTag[Transaction]]))
    //    val bitstampFetcher = builder.createRef(Props(classOf[PullFetchComponent[Transaction]], bitstampTransactionPullFetcher, implicitly[ClassTag[Transaction]]))
    val btceOrderFetcher = builder.createRef(Props(classOf[PullFetchComponent[Order]], btceOrderPullFetcher, implicitly[ClassTag[Order]]))
    //    val bitstampOrderFetcher = builder.createRef(Props(classOf[PullFetchListComponent[LimitOrder]], bitstampOrderPullFetcher, implicitly[ClassTag[List[LimitOrder]]]))
    // trading agents
    val arbitrageur = builder.createRef(Props(classOf[Arbitrageur], 111L, btceMarketId, bitstampMarketId))

    // Create the connections
    // BTC-e
    //    btceFetcher.addDestination(printer, classOf[Transaction])
    //    btceFetcher.addDestination(btcePersistor, classOf[Transaction])
    //    btceFetcher.addDestination(arbitrageur, classOf[Transaction])
//    btceOrderFetcher.addDestination(printer, classOf[LiveLimitAskOrder])
//    btceOrderFetcher.addDestination(printer, classOf[DelOrder])
//    btceOrderFetcher.addDestination(printer, classOf[LiveLimitBidOrder])
    // Bitstamp
    //    bitstampFetcher.addDestination(printer, classOf[Transaction])
    //    bitstampFetcher.addDestination(arbitrageur, classOf[Transaction])
    //    bitstampFetcher.addDestination(bitstampPersistor, classOf[Transaction])
    //    bitstampOrderFetcher.addDestination(printer, classOf[List[LimitOrder]])

    // Start the system
    builder.start
  }
}