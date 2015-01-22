package ch.epfl.ts.example

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.fetch.{BitstampOrderPullFetcher, BitstampTransactionPullFetcher, BtceOrderPullFetcher, BtceTransactionPullFetcher, MarketNames, PullFetchComponent}
import ch.epfl.ts.component.persist.TransactionPersistor
import ch.epfl.ts.component.utils.{BackLoop, Printer}
import ch.epfl.ts.data.{DelOrder, LiveLimitAskOrder, LiveLimitBidOrder, OHLC, Order, Transaction}
import ch.epfl.ts.engine.{MarketRules, MarketSimulator}
import ch.epfl.ts.indicators.OhlcIndicator
import ch.epfl.ts.traders.Arbitrageur

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
    val btceBackLoop = builder.createRef(Props(classOf[BackLoop], btceMarketId, btceXactPersit))
    val bitstampBackLoop = builder.createRef(Props(classOf[BackLoop], bitstampMarketId, bitstampXactPersist))
    // OHLC indicators
    val ohlcIntervalMillis = 10000L
    val btceOhlc = builder.createRef(Props(classOf[OhlcIndicator], btceMarketId, ohlcIntervalMillis))
    val bitstampOhlc = builder.createRef(Props(classOf[OhlcIndicator], bitstampMarketId, ohlcIntervalMillis))
    // printer
    val printer = builder.createRef(Props(classOf[Printer], "my-printer"))

    // Create the connections
    // BTC-e
    // fetcher to market
    btceOrderFetcher.addDestination(btceMarket, classOf[LiveLimitAskOrder])
    btceOrderFetcher.addDestination(btceMarket, classOf[LiveLimitBidOrder])
    btceOrderFetcher.addDestination(btceMarket, classOf[DelOrder])
    // fetcher to backloop
    btceTransactionFetcher.addDestination(btceBackLoop, classOf[Transaction])
    // fetcher to OHLC
    btceTransactionFetcher.addDestination(btceOhlc, classOf[Transaction])
    // market to backloop
    btceMarket.addDestination(btceBackLoop, classOf[Transaction])
    // backloop to arbitrageur
    btceBackLoop.addDestination(arbitrageur, classOf[Transaction])
    // ohlc to arbitrageur
    btceOhlc.addDestination(arbitrageur, classOf[OHLC])
    // Bitstamp
    // fetcher to market
    bitstampOrderFetcher.addDestination(bitstampMarket, classOf[LiveLimitAskOrder])
    bitstampOrderFetcher.addDestination(bitstampMarket, classOf[LiveLimitBidOrder])
    bitstampOrderFetcher.addDestination(bitstampMarket, classOf[DelOrder])
    // fetcher to backloop
    bitstampTransactionFetcher.addDestination(bitstampBackLoop, classOf[Transaction])
    // fetcher to OHLC
    bitstampTransactionFetcher.addDestination(bitstampOhlc, classOf[Transaction])
    // market to backloop
    bitstampMarket.addDestination(bitstampBackLoop, classOf[Transaction])
    // backloop to arbitrageur
    bitstampBackLoop.addDestination(arbitrageur, classOf[Transaction])
    bitstampBackLoop.addDestination(arbitrageur, classOf[OHLC])
    // ohlc to arbitrageur
    bitstampOhlc.addDestination(arbitrageur, classOf[OHLC])
    // Start the system
    builder.start
  }
}