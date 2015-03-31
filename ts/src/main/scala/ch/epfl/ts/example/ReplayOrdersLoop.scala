package ch.epfl.ts.example

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.persist.{OrderPersistor, TransactionPersistor}
import ch.epfl.ts.component.replay.{Replay, ReplayConfig}
import ch.epfl.ts.component.utils.{BackLoop, Printer}
import ch.epfl.ts.data.{DelOrder, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, OHLC, Order, Transaction}
import ch.epfl.ts.engine.{MarketRules, OrderBookMarketSimulator, RevenueCompute}
import ch.epfl.ts.indicators.{OhlcIndicator, SMA, SmaIndicator}
import ch.epfl.ts.traders.{DoubleCrossoverTrader, DoubleEnvelopeTrader, SimpleTrader, SobiTrader, TransactionVwapTrader}

import scala.reflect.ClassTag

/**
 * Use case where orders are loaded from a persistor (previously filled with orders from
 * finance.csv) and fed into the MarketSimulator.
 * The transactions executed by the MS are saved in a TransactionsPersistor.
 * A Simple Trader, SOBI trader, VWAP trader, Double Envelope
 * trader and a Double Crossover trader are plugged in the system
 * and submit orders according to their defined strategies.
 * A RevenueCompute component periodically displays the revenue
 * of each trader.
 * 
 */
object ReplayOrdersLoop {

  def main(args: Array[String]) {
    implicit val builder = new ComponentBuilder("ReplayFinanceSystem")

    // replayer params
    val initTime = 25210389L
    val compression = 0.001
    // market params
    val marketId = 0L
    val rules = new MarketRules()

    // Persistors
    // source
    val financePersistor = new OrderPersistor("finance") // requires to have run CSVFetcher on finance.csv (obtained by mail from Milos)
    financePersistor.init()
    // destination
    val transactionsPersistor = new TransactionPersistor("ReplayTransactions")
    transactionsPersistor.init()

    // Create components
    // market
    val market = builder.createRef(Props(classOf[OrderBookMarketSimulator], marketId, rules), "market")
    // Replay
    val replayer = builder.createRef(Props(classOf[Replay[Order]], financePersistor, ReplayConfig(initTime, compression), implicitly[ClassTag[Order]]), "replayer")
    // Printer
    val printer = builder.createRef(Props(classOf[Printer], "ReplayLoopPrinter"), "printer")
    // backloop
    val backloop = builder.createRef(Props(classOf[BackLoop], marketId, transactionsPersistor), "backloop")
    // ohlc computation
    val shortTickSizeMillis = 5000L
    val ohlcShort = builder.createRef(Props(classOf[OhlcIndicator], marketId, shortTickSizeMillis), "ohlcShort")
    val longTickSizeMillis = 10000L
    val ohlclong = builder.createRef(Props(classOf[OhlcIndicator], marketId, longTickSizeMillis), "ohlcLong")
    // Indicators
    val shortPeriod = 5
    val longPeriod = 10
    val smaShort = builder.createRef(Props(classOf[SmaIndicator], shortPeriod), "smaShort")
    val smaLong = builder.createRef(Props(classOf[SmaIndicator], longPeriod), "smaLong")
    // Traders
    val traderNames: Map[Long, String] = Map(0L -> "Finance", 123L -> "SobiTrader", 132L -> "SimpleTrader", 333L -> "VwapTrader", 444L -> "DcTrader", 555L -> "DeTrader")
    val sobiTrader = builder.createRef(Props(classOf[SobiTrader], 123L, 3000, 2, 700.0, 50, 100.0, rules), "sobiTrader")
    val simpleTrader = builder.createRef(Props(classOf[SimpleTrader], 132L, 10000, 50.0), "simpleTrader")
    val transactionVwap = builder.createRef(Props(classOf[TransactionVwapTrader], 333L, longTickSizeMillis.toInt), "transactionVwapTrader")
    val dcTrader = builder.createRef(Props(classOf[DoubleCrossoverTrader], 444L, 5, 10, 50.0), "dcTrader")
    val deTrader = builder.createRef(Props(classOf[DoubleEnvelopeTrader], 555L, 0.025, 50.0), "deTrader")
    // Display
    val display = builder.createRef(Props(classOf[RevenueCompute], 5000, traderNames), "display")

    // Create connections
    // replay
    replayer.addDestination(market, classOf[Order])
    replayer.addDestination(market, classOf[LimitAskOrder])
    replayer.addDestination(market, classOf[LimitBidOrder])
    replayer.addDestination(market, classOf[DelOrder])
    // market
    market.addDestination(backloop, classOf[Transaction])
    market.addDestination(backloop, classOf[LimitBidOrder])
    market.addDestination(backloop, classOf[LimitAskOrder])
    market.addDestination(backloop, classOf[DelOrder])
    market.addDestination(display, classOf[Transaction])
    // backLoop
    backloop.addDestination(sobiTrader, classOf[LimitAskOrder])
    backloop.addDestination(sobiTrader, classOf[LimitBidOrder])
    backloop.addDestination(sobiTrader, classOf[DelOrder])
    backloop.addDestination(transactionVwap, classOf[Transaction])
    backloop.addDestination(ohlcShort, classOf[Transaction])
    backloop.addDestination(ohlclong, classOf[Transaction])
    // ohlc
    ohlclong.addDestination(smaLong, classOf[OHLC])
    ohlcShort.addDestination(smaShort, classOf[OHLC])
    // moving averages
    smaLong.addDestination(deTrader, classOf[SMA])
    smaShort.addDestination(dcTrader, classOf[SMA])
    smaLong.addDestination(dcTrader, classOf[SMA])
    // traders
    // simpleTrader
    simpleTrader.addDestination(market, classOf[MarketAskOrder])
    simpleTrader.addDestination(market, classOf[MarketBidOrder])
    // SobiTrader
    sobiTrader.addDestination(market, classOf[LimitBidOrder])
    sobiTrader.addDestination(market, classOf[LimitAskOrder])
    // Double Crossover Trader
    dcTrader.addDestination(market, classOf[MarketAskOrder])
    dcTrader.addDestination(market, classOf[MarketBidOrder])
    // Double Envelope Trader
    deTrader.addDestination(market, classOf[MarketBidOrder])
    deTrader.addDestination(market, classOf[MarketAskOrder])
    // VWAP trader
    transactionVwap.addDestination(market, classOf[MarketAskOrder])
    transactionVwap.addDestination(market, classOf[MarketBidOrder])

    builder.start
  }

}