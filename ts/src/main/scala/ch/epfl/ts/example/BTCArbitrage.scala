package ch.epfl.ts.example

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.fetch.{ BitstampOrderPullFetcher, BitstampTransactionPullFetcher, BtceOrderPullFetcher, BtceTransactionPullFetcher, MarketNames, PullFetchComponent }
import ch.epfl.ts.component.persist.TransactionPersistor
import ch.epfl.ts.component.utils.{ BackLoop, Printer }
import ch.epfl.ts.data.{ DelOrder, LimitAskOrder, LimitBidOrder, OHLC, Order, Transaction, MarketAskOrder, MarketBidOrder }
import ch.epfl.ts.engine.{ MarketRules, OrderBookMarketSimulator }
import ch.epfl.ts.indicators.OhlcIndicator
import ch.epfl.ts.traders.Arbitrageur
import scala.reflect.ClassTag
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.data.MarketBidOrder

/**
 * in this system, two fetchers gather orders and transaction
 * and orders data from BTC-e and Bitstamp exchanges and feed them
 * to the MarketSimulator.
 * An arbitrageur trader receives the data from the Backloop,
 * monitors the price difference and submits orders in order to
 * make revenue.
 */
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
    val btceTransactionFetcher = builder.createRef(Props(classOf[PullFetchComponent[Transaction]], btceTransactionPullFetcher, implicitly[ClassTag[Transaction]]), "btceTransactionsFetcher")
    val bitstampTransactionFetcher = builder.createRef(Props(classOf[PullFetchComponent[Transaction]], bitstampTransactionPullFetcher, implicitly[ClassTag[Transaction]]), "bitstampTransactionFetcher")
    val btceOrderFetcher = builder.createRef(Props(classOf[PullFetchComponent[Order]], btceOrderPullFetcher, implicitly[ClassTag[Order]]), "btceOrderFetcher")
    val bitstampOrderFetcher = builder.createRef(Props(classOf[PullFetchComponent[Order]], bitstampOrderPullFetcher, implicitly[ClassTag[Order]]), "bitstampOrderFetcher")
    // trading agents
    val arbitrageurId = 111L
    val tradingPriceDelta = 1.0
    val volume = 50.0
    val arbitrageur = builder.createRef(Props(classOf[Arbitrageur], 111L, tradingPriceDelta, volume), "arbitrageur")
    // markets
    val rules = new MarketRules()
    val btceMarket = builder.createRef(Props(classOf[OrderBookMarketSimulator], btceMarketId, rules), MarketNames.BTCE_NAME)
    val bitstampMarket = builder.createRef(Props(classOf[OrderBookMarketSimulator], bitstampMarketId, rules), MarketNames.BITSTAMP_NAME)
    // backloops
    val btceBackLoop = builder.createRef(Props(classOf[BackLoop], btceMarketId, btceXactPersit), "btceBackLoop")
    val bitstampBackLoop = builder.createRef(Props(classOf[BackLoop], bitstampMarketId, bitstampXactPersist), "bitstampBackLoop")

    // Create the connections
    // BTC-e
    // fetcher to market
    btceOrderFetcher.addDestination(btceMarket, classOf[LimitAskOrder])
    btceOrderFetcher.addDestination(btceMarket, classOf[LimitBidOrder])
    btceOrderFetcher.addDestination(btceMarket, classOf[DelOrder])
    // fetcher to backloop
    btceTransactionFetcher.addDestination(btceBackLoop, classOf[Transaction])
    // market to backloop
    btceMarket.addDestination(btceBackLoop, classOf[Transaction])
    // backloop to arbitrageur
    btceBackLoop.addDestination(arbitrageur, classOf[Transaction])
    // Bitstamp
    // fetcher to market
    bitstampOrderFetcher.addDestination(bitstampMarket, classOf[LimitAskOrder])
    bitstampOrderFetcher.addDestination(bitstampMarket, classOf[LimitBidOrder])
    bitstampOrderFetcher.addDestination(bitstampMarket, classOf[DelOrder])
    // fetcher to backloop
    bitstampTransactionFetcher.addDestination(bitstampBackLoop, classOf[Transaction])
    // market to backloop
    bitstampMarket.addDestination(bitstampBackLoop, classOf[Transaction])
    // backloop to arbitrageur
    bitstampBackLoop.addDestination(arbitrageur, classOf[Transaction])
    bitstampBackLoop.addDestination(arbitrageur, classOf[OHLC])
    // Arbitrageur to markets
    arbitrageur.addDestination(btceMarket, classOf[MarketBidOrder])
    arbitrageur.addDestination(btceMarket, classOf[MarketAskOrder])
    arbitrageur.addDestination(bitstampMarket, classOf[MarketAskOrder])
    arbitrageur.addDestination(bitstampMarket, classOf[MarketBidOrder])
    // Start the system
    builder.start
  }
}