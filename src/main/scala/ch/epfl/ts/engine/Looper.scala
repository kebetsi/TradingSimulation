package ch.epfl.ts.engine

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.{ Transaction, Order }
import ch.epfl.ts.component.persist.Persistance

/**
 * Backloop component, plugged as Market Simulator's output. Saves the transactions in a persistor.
 * distributes the transactions and delta orders to the trading agents
 */
class Looper(p: Persistance[Transaction]) extends Component {

  override def receiver = {
    case t: Transaction => {
      p.save(t)
      send(t)
    }
    case o: Order => send(o)
    case _        => println("Looper: received unknown")
  }
}