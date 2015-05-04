package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{DelOrder, LimitAskOrder, LimitBidOrder, Order, Transaction}
import ch.epfl.ts.engine.{MarketRules, OrderBook}
import scala.collection.mutable.TreeSet
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.data.TimeParameter
import ch.epfl.ts.data.NaturalNumberParameter
import ch.epfl.ts.data.RealNumberParameter
import scala.concurrent.duration.FiniteDuration
import ch.epfl.ts.data.MarketRulesParameter

/**
 * SobiTrader companion object
 */
object SobiTrader extends TraderCompanion {
  type ConcreteTrader = SobiTrader
  override protected val concreteTraderTag = scala.reflect.classTag[SobiTrader]
   
  /**
   * Difference in price
   */
  val PRICE_DELTA = "PriceDelta"
  /**
   * @TODO Documentation
   */
  val THETA = "Theta"
  /**
   * Volume to be traded
   */
  val VOLUME = "Volume"
  /**
   * Time interval
   */
  val INTERVAL = "Interval"
  /**
   * Number of quartiles to take into account in the computation
   */
  val QUARTILES = "Quartiles"
  /**
   * Market rules of the market we are trading on
   */
  val MARKET_RULES = "MarketRules"
  
  override def strategyRequiredParameters = Map(
    THETA -> RealNumberParameter,
    PRICE_DELTA -> RealNumberParameter,
    VOLUME -> NaturalNumberParameter,
    INTERVAL -> TimeParameter,
    QUARTILES -> NaturalNumberParameter,
    MARKET_RULES -> MarketRulesParameter
  )
}

/**
 * SOBI trader
 */
class SobiTrader(uid: Long, parameters: StrategyParameters) extends Trader(uid, parameters) {
  import context._
  case object PossibleOrder

  override def companion = SobiTrader
  
  val theta = parameters.get[Double](SobiTrader.THETA)
  val priceDelta = parameters.get[Double](SobiTrader.PRICE_DELTA)
  val volume = parameters.get[Int](SobiTrader.VOLUME)
  val interval = parameters.get[FiniteDuration](SobiTrader.INTERVAL)
  val quartiles = parameters.get[Int](SobiTrader.QUARTILES)
  val rules = parameters.get[MarketRules](SobiTrader.MARKET_RULES)
  
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
        println("SobiTrader: making buy order: price=" + (tradingPrice - priceDelta) + ", volume=" + volume)
        send[Order](LimitBidOrder(currentOrderId, uid, System.currentTimeMillis, USD, USD, volume, tradingPrice - priceDelta))
      }
      if ((bi - si) > theta) {
        currentOrderId = currentOrderId + 1
        //"place an order to sell x shares at (lastPrice+p)"
        println("SobiTrader: making sell order: price=" + (tradingPrice + priceDelta) + ", volume=" + volume)
        send[Order](LimitAskOrder(currentOrderId, uid, System.currentTimeMillis(), USD, USD, volume, tradingPrice + priceDelta))
      }
    }

    case _ => println("SobiTrader: received unknown")
  }

  override def init = {
    system.scheduler.schedule(0 milliseconds, interval, self, PossibleOrder)
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
      for (i <- 0 to (bids.size * (quartiles / 4))) {
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