package ch.epfl.ts.engine

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.{ Transaction, Order }
import ch.epfl.ts.component.persist.Persistance
import ch.epfl.ts.data.LimitAskOrder
import ch.epfl.ts.data.LimitBidOrder
import ch.epfl.ts.data.DelOrder

/**
 * Backloop component, plugged as Market Simulator's output. Saves the transactions in a persistor.
 * distributes the transactions and delta orders to the trading agents
 */
class BackLoop(p: Persistance[Transaction]) extends Component {

  override def receiver = {
    case t: Transaction => {
      p.save(t)
      send(t)
    }
    case la: LimitAskOrder => send(la)
    case lb: LimitBidOrder => send(lb)
    case d: DelOrder       => send(d)
    case _                 => println("Looper: received unknown")
  }
}