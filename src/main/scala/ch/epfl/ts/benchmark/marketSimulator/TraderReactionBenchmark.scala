package ch.epfl.ts.benchmark.marketSimulator

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.persist.TransactionPersistor
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{DelOrder, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, Order, Transaction}
import ch.epfl.ts.component.utils.BackLoop

/**
 * The goal of this test is to measure the time it takes for a trader's order to be executed 
 * since the moment when the order that will trigger the trader's action is sent directly to 
 * the MarketSimulator.
 */
object TraderReactionBenchmark {

  def main(args: Array[String]) {
    var orders: List[Order] = Nil
    orders = MarketAskOrder(0L, 0L, System.currentTimeMillis(), BTC, USD, 50.0, 0.0) :: orders
    orders = LimitBidOrder(0L, 0L, System.currentTimeMillis(), BTC, USD, 50.0, 50.0) :: orders

    // create factory
    implicit val builder = new ComponentBuilder("MarketSimulatorBenchmarkSystem")

    // Persistor
    val persistor = new TransactionPersistor("bench-persistor")
    persistor.init()

    // Create Components
    val orderFeeder = builder.createRef(Props(classOf[OrderFeeder], orders), "orderFeeder")
    val market = builder.createRef(Props(classOf[BenchmarkOrderBookMarketSimulator], 1L, new BenchmarkMarketRules()), "market")
    val backloop = builder.createRef(Props(classOf[BackLoop], 1L, persistor), "backloop")
    val trader = builder.createRef(Props(classOf[BenchmarkTrader]), "trader")
    val timeCounter = builder.createRef(Props(classOf[TimeCounter]), "timeCounter")

    // Create Connections
    //orders
    orderFeeder.addDestination(market, classOf[LimitAskOrder])
    orderFeeder.addDestination(market, classOf[LimitBidOrder])
    orderFeeder.addDestination(market, classOf[MarketAskOrder])
    orderFeeder.addDestination(market, classOf[MarketBidOrder])
    orderFeeder.addDestination(market, classOf[DelOrder])
    orderFeeder.addDestination(market, classOf[LastOrder])
    // used to test without the backloop
//    market.addDestination(trader, classOf[Transaction])
    market.addDestination(backloop, classOf[Transaction])
    backloop.addDestination(trader, classOf[Transaction])
    trader.addDestination(market, classOf[LastOrder])
    // start and end signals
    orderFeeder.addDestination(timeCounter, classOf[StartSending])
    market.addDestination(timeCounter, classOf[FinishedProcessingOrders])

    // start the benchmark
    builder.start
  }
}