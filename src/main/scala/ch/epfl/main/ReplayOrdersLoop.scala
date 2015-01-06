package ch.epfl.main

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import ch.epfl.ts.component.fetch.SimulatorBackLoop
import ch.epfl.ts.component.persist.{ TransactionPersistor, OrderPersistor }
import ch.epfl.ts.component.replay.{ Replay, ReplayConfig }
import ch.epfl.ts.data.OrderType._
import ch.epfl.ts.engine.{ MarketSimulator, MarketRules, Looper }
import ch.epfl.ts.component.{ ComponentBuilder, Component }
import scala.reflect.ClassTag
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.traders.{ SobiTrader, SimpleTrader }
import ch.epfl.ts.data.{ Order, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, DelOrder, Transaction }
import ch.epfl.ts.data.LimitAskOrder
import ch.epfl.ts.data.LimitBidOrder
import ch.epfl.ts.data.DelOrder

object ReplayOrdersLoop {

  def main(args: Array[String]) {
    println("dwaddwadwawwa")
    val initTime = 25210389L
    val compression = 0.001
    val rules = new MarketRules()
    implicit val builder = new ComponentBuilder("ReplayFinanceSystem")
    val market = builder.createRef(Props(classOf[MarketSimulator], rules))
    val financePersistor = new OrderPersistor("finance") // requires to have run CSVFetcher on finance.csv (obtained by mail from Milos)
    financePersistor.init()
    val transactionsPersistor = new TransactionPersistor("ReplayTransactions")
    val replayer = builder.createRef(Props(classOf[Replay[Order]], financePersistor, ReplayConfig(initTime, compression), implicitly[ClassTag[Order]]))
    val sobiTrader = builder.createRef(Props(classOf[SobiTrader], 3000, 2, 700.0, 50, 100.0, rules))
    val simpleTrader = builder.createRef(Props(classOf[SimpleTrader], 10000, 50.0))
    val printer = builder.createRef(Props(classOf[Printer], "ReplayLoopPrinter"))
    val backloop = builder.createRef(Props(classOf[Looper], transactionsPersistor))

    replayer.addDestination(market, classOf[LimitAskOrder])
    replayer.addDestination(market, classOf[LimitBidOrder])
    replayer.addDestination(market, classOf[DelOrder])
    simpleTrader.addDestination(market, classOf[MarketBidOrder])
    simpleTrader.addDestination(market, classOf[MarketAskOrder])
    sobiTrader.addDestination(market, classOf[LimitBidOrder])
    sobiTrader.addDestination(market, classOf[LimitAskOrder])
    market.addDestination(backloop, classOf[Transaction])
    market.addDestination(backloop, classOf[LimitBidOrder])
    market.addDestination(backloop, classOf[LimitAskOrder])
    market.addDestination(backloop, classOf[DelOrder])
    backloop.addDestination(sobiTrader, classOf[LimitAskOrder])
    backloop.addDestination(sobiTrader, classOf[LimitBidOrder])
    backloop.addDestination(sobiTrader, classOf[DelOrder])
    
    builder.start
  }

}