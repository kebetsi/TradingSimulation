package ch.epfl.ts.example

import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.data.{ Order, DelOrder, LiveLimitAskOrder, LiveLimitBidOrder, Transaction, OHLC }
import ch.epfl.ts.engine.{ BackLoop, MarketSimulator, MarketRules }
import ch.epfl.ts.traders.Arbitrageur
import ch.epfl.ts.component.fetch.{ PullFetchComponent, BitstampTransactionPullFetcher, BitstampOrderPullFetcher, BtceTransactionPullFetcher, BtceOrderPullFetcher, MarketNames }
import ch.epfl.ts.component.persist.TransactionPersistor
import ch.epfl.ts.component.ComponentBuilder
import akka.actor.Props
import scala.reflect.ClassTag

object BTCArbitrage {

  def main(args: Array[String]) {
    implicit val builder = new ComponentBuilder("ArbitrageSystem")

    // Initialize the Interfaces to the DBs
    val btceXactPersit = new TransactionPersistor("btce-transaction-db2")
    btceXactPersit.init()
    val bitstampXactPersist = new TransactionPersistor("bitstamp-transaction-db2")
    bitstampXactPersist.init()

    // Instantiate Transaction fetchers for Bitcoin exchange markets
    val btceMarketId = MarketNames.BTCE_ID
    val bitstampMarketId = MarketNames.BITSTAMP_ID
    val btceTransactionPullFetcher = new BtceTransactionPullFetcher
    val btceOrderPullFetcher = new BtceOrderPullFetcher
    val bitstampTransactionPullFetcher = new BitstampTransactionPullFetcher
    val bitstampOrderPullFetcher = new BitstampOrderPullFetcher

    // Create Components

    // fetchers
    val btceTransactionFetcher = builder.createRef(Props(classOf[PullFetchComponent[Transaction]], btceTransactionPullFetcher, implicitly[ClassTag[Transaction]]))
    val bitstampTransactionFetcher = builder.createRef(Props(classOf[PullFetchComponent[Transaction]], bitstampTransactionPullFetcher, implicitly[ClassTag[Transaction]]))
    val btceOrderFetcher = builder.createRef(Props(classOf[PullFetchComponent[Order]], btceOrderPullFetcher, implicitly[ClassTag[Order]]))
    val bitstampOrderFetcher = builder.createRef(Props(classOf[PullFetchComponent[Order]], bitstampOrderPullFetcher, implicitly[ClassTag[Order]]))
    // trading agents
    val arbitrageur = builder.createRef(Props(classOf[Arbitrageur], 111L))
    // markets
    val rules = new MarketRules()
    val btceMarket = builder.createRef(Props(classOf[MarketSimulator], btceMarketId, rules))
    val bitstampMarket = builder.createRef(Props(classOf[MarketSimulator], bitstampMarketId, rules))
    // backloops
    val btceOhlcIntervalMillis = 10000
    val btceBackLoop = builder.createRef(Props(classOf[BackLoop], btceMarketId, btceOhlcIntervalMillis, btceXactPersit))
    val bitstampOhlcIntervalMillis = 10000
    val bitstampBackLoop = builder.createRef(Props(classOf[BackLoop], bitstampMarketId, bitstampOhlcIntervalMillis, bitstampXactPersist))
    // printer
    val printer = builder.createRef(Props(classOf[Printer], "my-printer"))

    // Create the connections
    // BTC-e
    // fetcher to market
    btceOrderFetcher.addDestination(btceMarket, classOf[LiveLimitAskOrder])
    btceOrderFetcher.addDestination(btceMarket, classOf[LiveLimitBidOrder])
    btceOrderFetcher.addDestination(btceMarket, classOf[DelOrder])
    // market to backloop
    btceMarket.addDestination(btceBackLoop, classOf[Transaction])
    // fetcher to backloop
    btceTransactionFetcher.addDestination(btceBackLoop, classOf[Transaction])
    // backloop to arbitrageur
    btceBackLoop.addDestination(arbitrageur, classOf[Transaction])
    btceBackLoop.addDestination(arbitrageur, classOf[OHLC])
    // Bitstamp
    // fetcher to market
    bitstampOrderFetcher.addDestination(bitstampMarket, classOf[LiveLimitAskOrder])
    bitstampOrderFetcher.addDestination(bitstampMarket, classOf[LiveLimitBidOrder])
    bitstampOrderFetcher.addDestination(bitstampMarket, classOf[DelOrder])
    // market to backloop
    bitstampMarket.addDestination(bitstampBackLoop, classOf[Transaction])
    // fetcher to backloop
    bitstampTransactionFetcher.addDestination(bitstampBackLoop, classOf[Transaction])
    // backloop to arbitrageur
    bitstampBackLoop.addDestination(arbitrageur, classOf[Transaction])
    bitstampBackLoop.addDestination(arbitrageur, classOf[OHLC])

    // Start the system
    builder.start
  }
}