package ch.epfl.main

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.persist.{OrderPersistor, TransactionPersistor}
import ch.epfl.ts.component.replay.{Replay, ReplayConfig}
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.data.{DelOrder, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, Order, Transaction}
import ch.epfl.ts.engine.{BackLoop, MarketRules, MarketSimulator}
import ch.epfl.ts.traders.{SimpleTrader, SobiTrader, TransactionVwapTrader}

import scala.reflect.ClassTag

object ReplayOrdersLoop {

  def main(args: Array[String]) {
    val initTime = 25210389L
    val compression = 0.001
    val rules = new MarketRules()
    implicit val builder = new ComponentBuilder("ReplayFinanceSystem")
    val market = builder.createRef(Props(classOf[MarketSimulator], rules))
    val financePersistor = new OrderPersistor("finance") // requires to have run CSVFetcher on finance.csv (obtained by mail from Milos)
    financePersistor.init()
    val transactionsPersistor = new TransactionPersistor("ReplayTransactions")
    transactionsPersistor.init()
    val replayer = builder.createRef(Props(classOf[Replay[Order]], financePersistor, ReplayConfig(initTime, compression), implicitly[ClassTag[Order]]))
    val sobiTrader = builder.createRef(Props(classOf[SobiTrader], 3000, 2, 700.0, 50, 100.0, rules))
    val simpleTrader = builder.createRef(Props(classOf[SimpleTrader], 10000, 50.0))
    val printer = builder.createRef(Props(classOf[Printer], "ReplayLoopPrinter"))
    val backloop = builder.createRef(Props(classOf[BackLoop], transactionsPersistor))
    val transactionVwap = builder.createRef(Props(classOf[TransactionVwapTrader], 10000))

    replayer.addDestination(market, classOf[Order])
    simpleTrader.addDestination(market, classOf[Order])
    sobiTrader.addDestination(market, classOf[Order])
    market.addDestination(backloop, classOf[Transaction])
    market.addDestination(backloop, classOf[LimitBidOrder])
    market.addDestination(backloop, classOf[LimitAskOrder])
    market.addDestination(backloop, classOf[DelOrder])
    backloop.addDestination(sobiTrader, classOf[LimitAskOrder])
    backloop.addDestination(sobiTrader, classOf[LimitBidOrder])
    backloop.addDestination(sobiTrader, classOf[DelOrder])
    backloop.addDestination(transactionVwap, classOf[Transaction])
    transactionVwap.addDestination(market, classOf[MarketAskOrder])
    transactionVwap.addDestination(market, classOf[MarketBidOrder])
    
    builder.start
  }

}