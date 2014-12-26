package ch.epfl.ts.first

import akka.actor.ActorSystem
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.impl.TransactionPersistorImpl
import akka.actor.Props
import akka.actor.Actor
import ch.epfl.ts.impl.OrderPersistorImpl
import ch.epfl.ts.data.Order
import ch.epfl.ts.data.OrderType._
import ch.epfl.ts.engine.BidOrder
import ch.epfl.ts.engine.AskOrder
import ch.epfl.ts.engine.DelOrder
import ch.epfl.ts.engine.MarketSimulator
import akka.actor.ActorRef

object ReplayOrdersLoop {

  class MarketConnector(out: List[ActorRef]) extends Actor {
    override def receive = {
      case o: Order => {
        println("Connector: " + System.currentTimeMillis, o.toString)
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
    val initTime = 25252541L
    val compression = 0.001
    val system = ActorSystem("ReplayFinanceSystem")
    val market = system.actorOf(Props(classOf[MarketSimulator]))
    val connector = system.actorOf(Props(classOf[MarketConnector], List(market)))
    val persistor = new OrderPersistorImpl("finance")
    persistor.init()
    val replay = system.actorOf(Props(classOf[Replay[Order]], persistor, List(connector), ReplayConfig(initTime, compression)))
    replay ! "Start"
  }
}