package ch.epfl.ts.component.utils

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.{ LimitOrder, DelOrder, LimitAskOrder, LimitBidOrder, Transaction, Tweet, LiveLimitAskOrder, LiveLimitBidOrder }
import ch.epfl.ts.data.OHLC

/**
 * Simple printer component for Transactions.
 * @param name The name of the component.
 */
class Printer(val name: String) extends Component {
  override def receiver = {
    case t: Transaction         => println("Printer " + name + ": Transaction\t" + System.currentTimeMillis + "\t" + t.toString)
    case t: Tweet               => println("Printer " + name + ": Tweet\t" + System.currentTimeMillis + "\t" + t.toString)
    case lb: LimitBidOrder      => println("Printer " + name + ": Limit Bid Order\t" + System.currentTimeMillis() + "\t" + lb.toString)
    case la: LimitAskOrder      => println("Printer " + name + ": Limit Ask Order\t" + System.currentTimeMillis() + "\t" + la.toString)
    case del: DelOrder          => println("Printer " + name + ": Delete Order\t" + System.currentTimeMillis() + "\t" + del.toString)
    case llb: LiveLimitBidOrder => println("Printer " + name + ": Live Limit Bid Order\t" + System.currentTimeMillis() + "\t" + llb.toString)
    case lla: LiveLimitAskOrder => println("Printer " + name + ": Live Limit Ask Order\t" + System.currentTimeMillis() + "\t" + lla.toString)
    case ohlc: OHLC             => println("Printer " + name + ": OHLC\t" + System.currentTimeMillis() + "\t" + ohlc.toString)
    case os: List[LimitOrder]   => println("Printer " + name + " LimitOrders"); os.map(x => println(x))
    case _                      => println("Printer " + name + ": received unknown")
  }
}
