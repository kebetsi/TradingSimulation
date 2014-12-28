package ch.epfl.main

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import ch.epfl.ts.component.persist.OrderPersistor
import ch.epfl.ts.component.replay.{Replay, ReplayConfig}
import ch.epfl.ts.data.OrderType._
import ch.epfl.ts.data.{Order, Transaction}
import ch.epfl.ts.engine.{AskOrder, BidOrder, DelOrder, MarketSimulator}
import ch.epfl.ts.whatisthis.SimulatorPushFetchImpl
import ch.epfl.ts.traders.SobiTrader

object ReplayOrdersLoop {

  class MarketConnector(out: List[ActorRef]) extends Actor {
    override def receive = {
      case o: Order => {
        println("Connector: " + System.currentTimeMillis + ", " + o.toString)
        o.orderType match {
          case BID => out.map { _ ! new BidOrder(1, o.id, o.timestamp, o.currency, o.price, o.quantity, o.currency) }
          case ASK => out.map { _ ! new AskOrder(1, o.id, o.timestamp, o.currency, o.price, o.quantity, o.currency) }
          case DEL => out.map { _ ! new DelOrder(1, o.id, o.timestamp, o.currency, o.price, o.quantity, o.currency) }
          case _   => println("Printer: order with unknown type received")
        }
      }
      case _ => {
        print("Connector: unknown thing received: ")
      }
    }
  }

  def main(args: Array[String]) {
    val initTime = 28320299L
    val compression = 0.00001
    val system = ActorSystem("ReplayFinanceSystem")
    val market = system.actorOf(Props(classOf[MarketSimulator]))
    val connector = system.actorOf(Props(classOf[MarketConnector], List(market)))
    val persistor = new OrderPersistor("finance") // requires to have run CSVFetcher on finance.csv (obtained by mail from Milos)
    persistor.init()
    val replay = system.actorOf(Props(classOf[Replay[Order]], persistor, List(connector), ReplayConfig(initTime, compression)))
    //    val transactionsPersistor = system.actorOf(Props(new TransactionPersistorImpl("persistance")))
    //    val transactionsPersistor = system.actorOf(Props(classOf[TransactionPersistorImpl],"persistance"))

    //    val vwapTrader = system.actorOf(Props(classOf[VwapTrader], market))
//    (market: ActorRef, intervalMillis: Int, quartile: Int, theta: Double, orderVolume: Int, priceDelta: Double)
    val sobiTrader = system.actorOf(Props(classOf[SobiTrader], market, 3000, 2, 700.0, 50, 100.0))
    //    val pusher = system.actorOf(Props(classOf[SimulatorPushFetchImpl[Transaction]], List(market), List(sobiTrader, transactionsPersistor)))
    val pusher = system.actorOf(Props(classOf[SimulatorPushFetchImpl[Transaction]], List(market), List(sobiTrader)))

    pusher ! "Start" // sends his ActorRef to source (here market)
    replay ! "Start"
  }
}