package ch.epfl.main

import akka.actor.{ ActorSystem, Props }
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.engine.PrintBooks
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.StartSignal
import ch.epfl.ts.component.Component
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.data.{Transaction, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, Order, DelOrder}
import ch.epfl.ts.engine.{MarketRules, MarketSimulator}

class testOrdersSender extends Component {

  override def receiver = {
    case StartSignal() => {
      //      send(new LimitAskOrder(1, 1, 1, USD, 90, 50, USD))

      send(new LimitBidOrder(2, 1, 8, USD, USD, 100, 90))

      //      send(MarketAskOrder(3, 1, 2, USD, 0, 50, USD))
      //            send(MarketBidOrder(3, 1, 2, USD, 0, 50, USD))
      send(LimitAskOrder(1, 3, 3, USD, USD, 50, 110))
      send(LimitAskOrder(1, 3, 1, USD, USD, 50, 110))
      send(LimitAskOrder(1, 4, 4, USD, USD, 50, 120))
      send(LimitAskOrder(1, 5, 5, USD, USD, 50, 110))
      send(LimitAskOrder(1, 6, 6, USD, USD, 50, 140))
      send(LimitAskOrder(1, 7, 7, USD, USD, 50, 80))
      send(LimitAskOrder(1, 2, 2, USD, USD, 50, 90))

    }
    case _ => {
      print("Connector: unknown thing received: ")
    }
  }
}

object MarketSimulatorTest {

  def main(args: Array[String]) {
    println("daw")
    //
    //    val system = ActorSystem("marketSystem")
    //    val market = system.actorOf(Props(new MarketSimulator), "market")

    implicit val builder = new ComponentBuilder("ReplayFinanceSystem")
    val market = builder.createRef(Props(classOf[MarketSimulator], MarketRules()))
    val tester = builder.createRef(Props(classOf[testOrdersSender]))
    val printer = builder.createRef(Props(classOf[Printer], "ReplayLoopPrinter"))
    market.addDestination(printer, classOf[Transaction])
    tester.addDestination(market, classOf[LimitAskOrder])
    tester.addDestination(market, classOf[LimitBidOrder])
    tester.addDestination(market, classOf[MarketBidOrder])
    tester.addDestination(market, classOf[MarketAskOrder])
    builder.start

    //    market ! new LimitAskOrder(1, 1, 1,USD, 100, 50, USD)
    //    market ! new LimitAskOrder(1, 2, 2,USD, 90, 50, USD)
    //    market ! new LimitAskOrder(1, 3, 3, USD, 110, 50, USD)
    //    market ! new LimitAskOrder(1, 3, 1, USD, 110, 50, USD)
    //    market ! new LimitAskOrder(1, 4, 4, USD, 120, 50, USD)
    //    market ! new LimitAskOrder(1, 5, 5, USD, 110, 50, USD)
    //    market ! new LimitAskOrder(1, 6, 6, USD, 140, 50, USD)
    //    market ! new LimitAskOrder(1, 7, 7, USD, 80, 100, USD)

    //    market ! PrintBooks

    //    market ! new LimitBidOrder(2, 8, 8, USD, 90, 50, USD)
    //    market ! new LimitBidOrder(2, 9, 11, USD, 110, 50, USD)
    //    market ! new LimitBidOrder(2, 9, 5, USD, 110, 50, USD)
    //    market ! new LimitBidOrder(2, 9, 9, USD, 110, 50, USD)
    //    market ! new LimitBidOrder(2, 10, 10, USD, 100, 50, USD)

    //    market ! PrintBooks
  }
}