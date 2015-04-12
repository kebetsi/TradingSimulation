package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{DelOrder, LimitAskOrder, LimitBidOrder, Order, Transaction}
import ch.epfl.ts.engine.{MarketRules, OrderBook}

import scala.collection.mutable.TreeSet
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/**
 * SOBI trader
 */
class SobiTrader(uid: Long, intervalMillis: Int, quartile: Int, theta: Double, orderVolume: Int, priceDelta: Double, rules: MarketRules)
  extends Component {
  import context._

  case object PossibleOrder

  val book = OrderBook(rules.bidsOrdering, rules.asksOrdering)
  var tradingPrice = 188700.0 // for finance.csv

  var bi: Double = 0.0
  var si: Double = 0.0
  var currentOrderId: Long = 456789

  override def receiver = {
    case limitAsk: LimitAskOrder  => book insertAskOrder limitAsk
    case limitBid: LimitBidOrder  => book insertBidOrder limitBid
    case delete: DelOrder         => removeOrder(delete)
    case transaction: Transaction => tradingPrice = transaction.price

    case PossibleOrder => {
      bi = computeBiOrSi(book.bids.book)
      si = computeBiOrSi(book.asks.book)
      if ((si - bi) > theta) {
        currentOrderId = currentOrderId + 1
        //"place an order to buy x shares at (lastPrice-p)"
        println("SobiTrader: making buy order: price=" + (tradingPrice - priceDelta) + ", volume=" + orderVolume)
        send[Order](LimitBidOrder(currentOrderId, uid, System.currentTimeMillis, USD, USD, orderVolume, tradingPrice - priceDelta))
      }
      if ((bi - si) > theta) {
        currentOrderId = currentOrderId + 1
        //"place an order to sell x shares at (lastPrice+p)"
        println("SobiTrader: making sell order: price=" + (tradingPrice + priceDelta) + ", volume=" + orderVolume)
        send[Order](LimitAskOrder(currentOrderId, uid, System.currentTimeMillis(), USD, USD, orderVolume, tradingPrice + priceDelta))
      }
    }

    case _ => println("SobiTrader: received unknown")
  }

  override def start = {
    system.scheduler.schedule(0 milliseconds, intervalMillis milliseconds, self, PossibleOrder)
  }

  def removeOrder(order: Order): Unit = book delete order

  /**
   * compute the volume-weighted average price of the top quartile*25% of the volume of the bids/asks orders book
   */
  def computeBiOrSi[T <: Order](bids: TreeSet[T]): Double = {
    if (bids.size > 4) {
      val it = bids.iterator
      var bi: Double = 0.0
      var vol: Double = 0
      for (i <- 0 to ((bids.size * quartile) / 4)) {
        val currentBidOrder = it.next()
        bi = bi + currentBidOrder.price * currentBidOrder.volume
        vol = vol + currentBidOrder.volume
      }
      bi / vol
    } else {
      0.0
    }
  }
}