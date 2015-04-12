package ch.epfl.ts.component.utils

import ch.epfl.ts.component.Component
import ch.epfl.ts.component.persist.Persistance
import ch.epfl.ts.data.{DelOrder, LimitAskOrder, LimitBidOrder, Transaction}

/**
 * Backloop component, plugged as Market Simulator's output. Saves the transactions in a persistor.
 * distributes the transactions and delta orders to the trading agents
 */
class BackLoop(marketId: Long, p: Persistance[Transaction]) extends Component {

  override def receiver = {
    case t: Transaction => {
      send(t)
      p.save(t)
    }
    case la: LimitAskOrder => send(la)
    case lb: LimitBidOrder => send(lb)
    case d: DelOrder => send(d)
    case _ => println("Looper: received unknown")
  }
}