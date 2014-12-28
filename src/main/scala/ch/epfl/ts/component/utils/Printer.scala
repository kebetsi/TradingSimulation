package ch.epfl.ts.component.utils

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.{Order, Tweet, Transaction}

/**
 * Simple printer component for Transactions.
 * @param name The name of the component.
 */
class Printer(val name: String) extends Component {
  override def receiver = {
    case t: Transaction => println("Transaction\t" + System.currentTimeMillis + "\t" + t.toString)
    case t: Tweet => println("Tweet\t" + System.currentTimeMillis + "\t" + t.toString)
    case o: Order => println("Order\t" + System.currentTimeMillis + "\t" + o.toString)
    case _ =>
  }
}
