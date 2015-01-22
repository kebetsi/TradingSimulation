package ch.epfl.ts.example

import akka.actor.Props
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.component.{Component, ComponentBuilder, StartSignal}
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, Transaction}
import ch.epfl.ts.engine.{MarketRules, MarketSimulator}
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.engine.MarketSimulator

/**
 * test setup of a MarketSimulator receiving orders and matching them when possible.
 */
object MarketSimulatorTest {

  def main(args: Array[String]) {
    // init builder
    implicit val builder = new ComponentBuilder("MarketSimTestSystem")
    
    // create components
    val market = builder.createRef(Props(classOf[MarketSimulator], 0L, new MarketRules()))
    val tester = builder.createRef(Props(classOf[testOrdersSender]))
    val printer = builder.createRef(Props(classOf[Printer], "MSprinter"))
    
    // create edges
    market.addDestination(printer, classOf[Transaction])
    tester.addDestination(market, classOf[LimitAskOrder])
    tester.addDestination(market, classOf[LimitBidOrder])
    tester.addDestination(market, classOf[MarketBidOrder])
    tester.addDestination(market, classOf[MarketAskOrder])
    
    // start system
    builder.start
  }
}

/**
 * simple component that sends a set of orders
 */
class testOrdersSender extends Component {

  override def receiver = {
    case StartSignal() => {

      send(LimitBidOrder(2, 1, 8, USD, USD, 100, 90))
      send(LimitAskOrder(1, 3, 1, USD, USD, 50, 110))
      send(LimitAskOrder(1, 4, 4, USD, USD, 50, 120))
      send(LimitAskOrder(1, 5, 5, USD, USD, 50, 110))
      send(LimitAskOrder(1, 6, 6, USD, USD, 50, 140))
      send(LimitAskOrder(1, 7, 7, USD, USD, 50, 80))
      send(LimitAskOrder(1, 2, 2, USD, USD, 50, 90))

    }
    case _ => {
      print("testOrdersSender: unknown data received")
    }
  }
}