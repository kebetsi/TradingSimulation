package ch.epfl.ts.example

import ch.epfl.ts.component.persist.TransactionPersistor
import ch.epfl.ts.component.persist.OrderPersistor
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.engine.MarketRules
import ch.epfl.ts.indicators.OhlcIndicator
import ch.epfl.ts.traders.DoubleEnvelopeTrader
import akka.actor.Props
import ch.epfl.ts.traders.TransactionVwapTrader
import ch.epfl.ts.traders.SobiTrader
import ch.epfl.ts.component.replay.Replay
import ch.epfl.ts.component.utils.BackLoop
import ch.epfl.ts.indicators.SmaIndicator
import ch.epfl.ts.traders.DoubleCrossoverTrader
import ch.epfl.ts.traders.SimpleTrader
import scala.reflect.ClassTag
import ch.epfl.ts.engine.{ RevenueCompute, MarketSimulator }
import ch.epfl.ts.component.replay.ReplayConfig
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.data.DelOrder
import ch.epfl.ts.data.LimitAskOrder
import ch.epfl.ts.data.OHLC
import ch.epfl.ts.indicators.SMA
import ch.epfl.ts.data.LimitBidOrder
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.Order

object ReplaySetup {

  def main(args: Array[String]) {

    implicit val builder = new ComponentBuilder("ReplayFinanceSystem")

    // replayer params
    val initTime = 25210389L
    val compression = 0.001

    // Persistors
    // source
    val financePersistor = new OrderPersistor("finance") // requires to have run CSVFetcher on finance.csv (obtained by mail from Milos)
    financePersistor.init()

    // Replay
    val replayer = builder.createRef(Props(classOf[Replay[Order]], financePersistor, ReplayConfig(initTime, compression), implicitly[ClassTag[Order]]))
    // Printer
    val printer = builder.createRef(Props(classOf[Printer], "ReplayLoopPrinter"))

    // Create connections
    // replay
//    replayer.addDestination(printer, classOf[Order])
    replayer.addDestination(printer, classOf[LimitAskOrder])
    replayer.addDestination(printer, classOf[LimitBidOrder])
    replayer.addDestination(printer, classOf[DelOrder])

    builder.start
  }

}