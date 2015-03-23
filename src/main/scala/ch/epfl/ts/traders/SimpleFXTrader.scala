package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.component.StartSignal
import ch.epfl.ts.data._
import scala.language.postfixOps
import scala.concurrent.duration.DurationInt
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.traders.SendMarketOrder
import ch.epfl.ts.component.StartSignal
import ch.epfl.ts.data.MarketAskOrder

class SimpleFXTrader(uid: Long) extends Component {
  import context._
  var orderId = 5500
  val initDelayMillis = 10000
  val intervalMillis = 5000;
  var alternate = 0

  override def receiver = {
    case StartSignal() => start
    case SendMarketOrder =>
      if (alternate % 2 == 0) {
        send[Order](MarketBidOrder(orderId, uid, System.currentTimeMillis(), Currency.EUR, Currency.USD, 30, 0))
      } else {
        send[Order](MarketAskOrder(orderId, uid, System.currentTimeMillis(), Currency.EUR, Currency.USD, 30, 0))
      }
      alternate=alternate+1
    case t: Transaction =>
      println("SimpleFXT: got %s -> %s for %f transaction info".format(t.whatC, t.withC, t.price))
    case q: Quote =>
    case _ =>
      println("SimpleFXT: unknown signal")
  }

  def start = {
    system.scheduler.schedule(initDelayMillis milliseconds, intervalMillis milliseconds, self, SendMarketOrder)
  }
}