package ch.epfl.ts.component.utils

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.{ Order, Tweet, Transaction, DelOrder, LimitAskOrder, LimitBidOrder }

/**
 * Simple printer component for Transactions.
 * @param name The name of the component.
 */
class Printer(val name: String) extends Component {
  override def receiver = {
    case t: Transaction    => println("Printer: Transaction\t" + System.currentTimeMillis + "\t" + t.toString)
    case t: Tweet          => println("Printer: Tweet\t" + System.currentTimeMillis + "\t" + t.toString)
    case lb: LimitBidOrder => println("Printer: Limit Bid Order\t" + System.currentTimeMillis() + "\t" + lb.toString)
    case la: LimitAskOrder => println("Printer: Limit Ask Order\t" + System.currentTimeMillis() + "\t" + la.toString)
    case del: DelOrder     => println("Printer: Delete Order\t" + System.currentTimeMillis() + "\t" + del.toString)
    case _                 => println("Printer: received unknown")
  }
}
