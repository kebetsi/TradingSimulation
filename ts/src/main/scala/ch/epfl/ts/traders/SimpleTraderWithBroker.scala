package ch.epfl.ts.traders

import ch.epfl.ts.component.{StartSignal, Component}
import ch.epfl.ts.data.{Register, Quote}

/**
 * Dummy broker-aware trader.
 */
class SimpleTraderWithBroker(uid: Long) extends Component{
  override def receiver = {
    case q: Quote => {
      println("TraderWithB receided a quote: " + q)
    }
    case s: StartSignal => {
      println("TraderWithB received startSignal")
      send(Register(uid))
    }
    case p => println("TraderWithB: received unknown " + p)
  }
}