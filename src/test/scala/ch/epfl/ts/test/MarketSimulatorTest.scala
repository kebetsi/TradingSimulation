package ch.epfl.ts.test

import ch.epfl.ts.engine.MarketSimulator
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.engine.BidOrder
import ch.epfl.ts.engine.AskOrder
import ch.epfl.ts.engine.Order
import ch.epfl.ts.engine.PrintBooks
import akka.actor.ActorSystem
import akka.actor.Props
import ch.epfl.ts.engine.PrintBooks
import ch.epfl.ts.engine.PrintBooks

object MarketSimulatorTest {

  def main(args: Array[String]) {

    val system = ActorSystem("marketSystem")
    val market = system.actorOf(Props(new MarketSimulator), "market")
//case class BidOrder(o: Order) extends Order(o.uid, o.whatC, o.price, o.quantity, o.withC)

    market ! new AskOrder(1,USD,100,50,USD)
    market ! new AskOrder(1, USD, 90, 50, USD)
    market ! new AskOrder(1, USD, 110, 50, USD)
    
    market ! PrintBooks
    
    market ! new BidOrder(2, USD, 90, 50, USD)
    market ! new BidOrder(2, USD, 110, 50, USD)
    market ! new BidOrder(2, USD, 100, 50, USD)

    market ! PrintBooks
//    system.shutdown()
  }
}