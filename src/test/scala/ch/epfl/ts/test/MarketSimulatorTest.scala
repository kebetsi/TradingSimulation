package ch.epfl.ts.test

import akka.actor.{ActorSystem, Props}
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.engine.{AskOrder, BidOrder, MarketSimulator, PrintBooks}

object MarketSimulatorTest {

  def main(args: Array[String]) {

    val system = ActorSystem("marketSystem")
    val market = system.actorOf(Props(new MarketSimulator), "market")

    market ! new AskOrder(1, 1, 1,USD, 100, 50, USD)
    market ! new AskOrder(1, 2, 2,USD, 90, 50, USD)
    market ! new AskOrder(1, 3, 3, USD, 110, 50, USD)
    market ! new AskOrder(1, 3, 1, USD, 110, 50, USD)
    market ! new AskOrder(1, 4, 4, USD, 120, 50, USD)
    market ! new AskOrder(1, 5, 5, USD, 110, 50, USD)
    market ! new AskOrder(1, 6, 6, USD, 140, 50, USD)
    market ! new AskOrder(1, 7, 7, USD, 80, 100, USD)

    market ! PrintBooks

    market ! new BidOrder(2, 8, 8, USD, 90, 50, USD)
    market ! new BidOrder(2, 9, 11, USD, 110, 50, USD)
    market ! new BidOrder(2, 9, 5, USD, 110, 50, USD)
    market ! new BidOrder(2, 9, 9, USD, 110, 50, USD)
    market ! new BidOrder(2, 10, 10, USD, 100, 50, USD)

    market ! PrintBooks
  }
}