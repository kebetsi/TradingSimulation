package ch.epfl.ts.traders

import ch.epfl.ts.component.{StartSignal, Component}
import ch.epfl.ts.data.{MarketAskOrder, ConfirmRegistration, Register, Quote}
import akka.actor.{ActorLogging, ActorRef}
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.Register
import ch.epfl.ts.data.Quote
import ch.epfl.ts.data.ConfirmRegistration
import ch.epfl.ts.data.MarketAskOrder

/**
 * Dummy broker-aware trader.
 */
class SimpleTraderWithBroker(uid: Long) extends Component with ActorLogging{
  val l = context.system.log
  var broker: ActorRef = null
  var registered = false
  var oid = 1L
  override def receiver = {
    case q: Quote => {
      println("TraderWithB receided a quote: " + q)
      sendSomeMarketOrder()
    }
    case ConfirmRegistration => {
      broker = sender()
      registered = true
      log.debug("TraderWithB: Broker confirmed")
    }
    case p => println("TraderWithB: received unknown: " + p)
  }

  override def start = {
    log.debug("TraderWithB received startSignal")
    send(Register(uid))
  }

  def sendSomeMarketOrder() = {//TODO(sygi): should it be asynchronous?
    send(MarketAskOrder(oid, uid, System.currentTimeMillis(), BTC, USD, 3.0, 14.0))
    oid = oid + 1
  }
}