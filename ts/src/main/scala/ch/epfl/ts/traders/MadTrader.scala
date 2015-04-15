package ch.epfl.ts.traders

import scala.language.postfixOps
import scala.util.Random
import scala.concurrent.duration.DurationInt
import ch.epfl.ts.data.LimitAskOrder
import ch.epfl.ts.data.LimitBidOrder
import ch.epfl.ts.data.Order
import ch.epfl.ts.component.StartSignal
import ch.epfl.ts.data.Currency

/**
 * Trader that gives just random ask and bid orders alternatively
 */
class MadTrader(uid: Long, intervalMillis: Int, orderVolume: Double) extends Trader {
  import context._
  private case object SendMarketOrder
  
  // TODO: this initial order Id should be unique in the system
  var orderId = 4567
  val initDelayMillis = 10000

  var alternate = 0
  val r = new Random

  override def receiver = {
    case StartSignal => start
    case SendMarketOrder => {
      if (alternate % 2 == 0) {
        println("SimpleTrader: sending market bid order")
        send[Order](LimitAskOrder(orderId, uid, System.currentTimeMillis(), Currency.USD, Currency.USD, 50, 10 + r.nextInt(10)))
      } else {
        println("SimpleTrader: sending market ask order")
        send[Order](LimitBidOrder(orderId, uid, System.currentTimeMillis(), Currency.USD, Currency.USD, 50, 10 + r.nextInt(10)))
      }
      alternate = alternate + 1
      orderId = orderId + 1
    }
    case _ => println("SimpleTrader: received unknown")
  }

  /**
   * When simulation is started, plan ahead the next random trade
   */
  override def start = {
    system.scheduler.schedule(initDelayMillis milliseconds, intervalMillis milliseconds, self, SendMarketOrder)
  }
}