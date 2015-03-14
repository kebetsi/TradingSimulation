package ch.epfl.ts.engine

import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{ DelOrder, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, Order, Streamable, Transaction }
import ch.epfl.ts.component.fetch.MarketNames
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.data.MarketAskOrder

/**
 * represents the cost of placing a bid and market order
 */
case class CommissionFX(limitOrderFee: Double, marketOrderFee: Double)

/**
 * Market Simulator Configuration class. Defines the orders books priority implementation, the matching function and the commission costs of limit and market orders.
 * Extend this class and override its method(s) to customize Market rules for specific markets.
 *
 */
class MarketFXRules {
  val commission = Commission(0, 0)

  // when used on TreeSet, head() and iterator() provide increasing order
  def asksOrdering = new Ordering[Order] {
    def compare(first: Order, second: Order): Int =
      if (first.price > second.price) 1
      else if (first.price < second.price) -1
      else {
        if (first.timestamp < second.timestamp) 1 else if (first.timestamp > second.timestamp) -1 else 0
      }
  }

  // when used on TreeSet, head() and iterator() provide decreasing order
  def bidsOrdering = new Ordering[Order] {
    def compare(first: Order, second: Order): Int =
      if (first.price > second.price) -1
      else if (first.price < second.price) 1
      else {
        if (first.timestamp < second.timestamp) 1 else if (first.timestamp > second.timestamp) -1 else 0
      }
  }

  def matchingFunction(marketId: Long,
                       newOrder: Order,
                       newOrdersBook: PartialOrderBook,
                       bestMatchsBook: PartialOrderBook,
                       send: Streamable => Unit,
                       currentTradingPrice: Double): Unit = {

    newOrder match {
      case mbid: MarketBidOrder =>
        println("Receive MBID order")
        //random order id destination/trader destination
        send(Transaction(marketId, currentTradingPrice, newOrder.volume, newOrder.timestamp, newOrder.whatC, newOrder.withC, newOrder.uid, newOrder.oid, 3L, 3L))
      case mask: MarketAskOrder =>
        //random order id destination/trader destination
        println("Receive MASK order")
        send(Transaction(marketId, currentTradingPrice, newOrder.volume, newOrder.timestamp, newOrder.whatC, newOrder.withC, 3L, 3L, newOrder.uid, newOrder.oid))

    }
  }
}
