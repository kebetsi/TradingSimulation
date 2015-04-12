package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{MarketAskOrder, MarketBidOrder, Order}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

case object SendMarketOrder

/**
 * Simple trader that periodically sends market ask and bid orders alternatively.
 */
class SimpleTrader(uid: Long, intervalMillis: Int, orderVolume: Double) extends Component {
  import context._

  var orderId = 4567
  val initDelayMillis = 10000

  var alternate = 0

  override def receiver = {
    case SendMarketOrder => {
      if (alternate % 2 == 0) {
        println("SimpleTrader: sending market bid order")
        send[Order](MarketBidOrder(orderId, uid, System.currentTimeMillis(), USD, USD, 50, 0))
      } else {
        println("SimpleTrader: sending market ask order")
        send[Order](MarketAskOrder(orderId, uid, System.currentTimeMillis(), USD, USD, 50, 0))
      }
      alternate = alternate + 1
      orderId = orderId + 1
    }
    case _ => println("SimpleTrader: received unknown")
  }

  override def start = {
    system.scheduler.schedule(initDelayMillis milliseconds, intervalMillis milliseconds, self, SendMarketOrder)
  }

}