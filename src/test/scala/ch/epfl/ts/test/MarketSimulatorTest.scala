package ch.epfl.ts.test

import akka.actor.{ActorSystem, Props}
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.engine.{AskOrder, BidOrder, MarketSimulator, PrintBooks}

object MarketSimulatorTest {

  def main(args: Array[String]) {

    val system = ActorSystem("marketSystem")
    val market = system.actorOf(Props(new MarketSimulator), "market")

    market ! new AskOrder(1, USD, 100, 50, USD)
    market ! new AskOrder(1, USD, 90, 50, USD)
    market ! new AskOrder(1, USD, 110, 50, USD)
    market ! new AskOrder(1, USD, 120, 50, USD)
    market ! new AskOrder(1, USD, 110, 50, USD)
    market ! new AskOrder(1, USD, 140, 50, USD)
    market ! new AskOrder(1, USD, 80, 50, USD)

    market ! PrintBooks

    market ! new BidOrder(2, USD, 90, 50, USD)
    market ! new BidOrder(2, USD, 110, 50, USD)
    market ! new BidOrder(2, USD, 100, 50, USD)

    market ! PrintBooks
  }
}