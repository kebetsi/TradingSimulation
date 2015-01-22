package ch.epfl.ts.example

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.persist.{OrderPersistor, TransactionPersistor}
import ch.epfl.ts.component.replay.{Replay, ReplayConfig}
import ch.epfl.ts.component.utils.{BackLoop, Printer}
import ch.epfl.ts.data.{DelOrder, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, OHLC, Order, Transaction}
import ch.epfl.ts.engine.{MarketRules, MarketSimulator, RevenueCompute}
import ch.epfl.ts.indicators.{OhlcIndicator, SMA, SmaIndicator}
import ch.epfl.ts.traders.{DoubleCrossoverTrader, DoubleEnvelopeTrader, SimpleTrader, SobiTrader, TransactionVwapTrader}

import scala.reflect.ClassTag

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
    val market = builder.createRef(Props(classOf[MarketSimulator], 1L, rules))
    // Replay
    val replayer = builder.createRef(Props(classOf[Replay[Order]], financePersistor, ReplayConfig(initTime, compression), implicitly[ClassTag[Order]]))
    // Printer
    val printer = builder.createRef(Props(classOf[Printer], "ReplayLoopPrinter"))
    // backloop
    val backloop = builder.createRef(Props(classOf[BackLoop], marketId, transactionsPersistor))
    // ohlc computation
    val shortTickSizeMillis = 5000L
    val ohlcShort = builder.createRef(Props(classOf[OhlcIndicator], marketId, shortTickSizeMillis))
    val longTickSizeMillis = 10000L
    val ohlclong = builder.createRef(Props(classOf[OhlcIndicator], marketId, longTickSizeMillis))
    // Indicators
    val shortPeriod = 5
    val longPeriod = 10
    val smaShort = builder.createRef(Props(classOf[SmaIndicator], shortPeriod))
    val smaLong = builder.createRef(Props(classOf[SmaIndicator], longPeriod))
    // Traders
    val sobiTrader = builder.createRef(Props(classOf[SobiTrader], 123L, 3000, 2, 700.0, 50, 100.0, rules))
    val simpleTrader = builder.createRef(Props(classOf[SimpleTrader], 132L, 10000, 50.0))
    val transactionVwap = builder.createRef(Props(classOf[TransactionVwapTrader], 333L, longTickSizeMillis.toInt))
    val dcTrader = builder.createRef(Props(classOf[DoubleCrossoverTrader], 444L, 5, 10, 50.0))
    val deTrader = builder.createRef(Props(classOf[DoubleEnvelopeTrader], 555L, 0.025, 50.0))
    // Display
    val display = builder.createRef(Props(classOf[RevenueCompute], 5000))

    // Create connections
    // replay
    replayer.addDestination(market, classOf[Order])
    // simpleTrader
    simpleTrader.addDestination(market, classOf[MarketAskOrder])
    simpleTrader.addDestination(market, classOf[MarketBidOrder])
    // SobiTrader
    sobiTrader.addDestination(market, classOf[LimitBidOrder])
    sobiTrader.addDestination(market, classOf[LimitAskOrder])
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
    dcTrader.addDestination(market, classOf[MarketAskOrder])
    dcTrader.addDestination(dcTrader, classOf[MarketBidOrder])
    transactionVwap.addDestination(market, classOf[MarketAskOrder])
    transactionVwap.addDestination(market, classOf[MarketBidOrder])

    builder.start
  }

}