package ch.epfl.main

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.persist.{ OrderPersistor, TransactionPersistor }
import ch.epfl.ts.component.replay.{ Replay, ReplayConfig }
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.traders.RevenueCompute
import ch.epfl.ts.data.{ DelOrder, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, Order, Transaction }
import ch.epfl.ts.engine.{ BackLoop, MarketRules, MarketSimulator }
import ch.epfl.ts.traders.{ SimpleTrader, SobiTrader, TransactionVwapTrader }
import scala.reflect.ClassTag
import ch.epfl.ts.indicators.SmaIndicator
import ch.epfl.ts.traders.DoubleCrossoverTrader
import ch.epfl.ts.engine.OHLC
import ch.epfl.ts.indicators.MA
import ch.epfl.ts.indicators.SMA
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.traders.DoubleEnvelopeTrader
import ch.epfl.ts.engine.BackLoop

object ReplayOrdersLoop {

  def main(args: Array[String]) {
    val initTime = 25210389L
    val ohlcTimeFrameMillis = 10000
    val compression = 0.001
    val rules = new MarketRules()
    implicit val builder = new ComponentBuilder("ReplayFinanceSystem")
    val market = builder.createRef(Props(classOf[MarketSimulator], 1L, rules))
    val financePersistor = new OrderPersistor("finance") // requires to have run CSVFetcher on finance.csv (obtained by mail from Milos)
    financePersistor.init()
    val transactionsPersistor = new TransactionPersistor("ReplayTransactions")
    transactionsPersistor.init()
    val replayer = builder.createRef(Props(classOf[Replay[Order]], financePersistor, ReplayConfig(initTime, compression), implicitly[ClassTag[Order]]))
    val sobiTrader = builder.createRef(Props(classOf[SobiTrader], 123L, 3000, 2, 700.0, 50, 100.0, rules))
    val simpleTrader = builder.createRef(Props(classOf[SimpleTrader], 132L, 10000, 50.0))
    val printer = builder.createRef(Props(classOf[Printer], "ReplayLoopPrinter"))
    val backloop = builder.createRef(Props(classOf[BackLoop], 10000, transactionsPersistor))
    val transactionVwap = builder.createRef(Props(classOf[TransactionVwapTrader], 333L, ohlcTimeFrameMillis))
    val display = builder.createRef(Props(classOf[RevenueCompute], 5000))
    val smaShort = builder.createRef(Props(classOf[SmaIndicator], ohlcTimeFrameMillis, 5))
    val smaLong = builder.createRef(Props(classOf[SmaIndicator], ohlcTimeFrameMillis, 10))
    val dcTrader = builder.createRef(Props(classOf[DoubleCrossoverTrader], 444L, 5, 10, 50.0))
    val deTrader = builder.createRef(Props(classOf[DoubleEnvelopeTrader], 555L, 0.025, 50.0))
        

    replayer.addDestination(market, classOf[Order])
    //    simpleTrader.addDestination(market, classOf[Order])
    simpleTrader.addDestination(market, classOf[MarketAskOrder])
    simpleTrader.addDestination(market, classOf[MarketBidOrder])
//    sobiTrader.addDestination(market, classOf[Order])
    sobiTrader.addDestination(market, classOf[LimitBidOrder])
    sobiTrader.addDestination(market, classOf[LimitAskOrder])
    market.addDestination(backloop, classOf[Transaction])
    //    market.addDestination(backloop, classOf[Order])
    market.addDestination(backloop, classOf[LimitBidOrder])
    market.addDestination(backloop, classOf[LimitAskOrder])
    market.addDestination(backloop, classOf[DelOrder])
    market.addDestination(display, classOf[Transaction])
    backloop.addDestination(sobiTrader, classOf[LimitAskOrder])
    backloop.addDestination(sobiTrader, classOf[LimitBidOrder])
    backloop.addDestination(sobiTrader, classOf[DelOrder])
    backloop.addDestination(transactionVwap, classOf[Transaction])
    backloop.addDestination(smaShort, classOf[OHLC])
    backloop.addDestination(smaLong, classOf[OHLC])
    backloop.addDestination(deTrader, classOf[OHLC])
    smaLong.addDestination(deTrader, classOf[SMA])
    smaShort.addDestination(dcTrader, classOf[SMA])
    smaLong.addDestination(dcTrader, classOf[SMA])
    dcTrader.addDestination(market, classOf[MarketAskOrder])
    dcTrader.addDestination(dcTrader, classOf[MarketBidOrder])
    //    transactionVwap.addDestination(market, classOf[Order])
    transactionVwap.addDestination(market, classOf[MarketAskOrder])
    transactionVwap.addDestination(market, classOf[MarketBidOrder])
    

    builder.start
  }

}