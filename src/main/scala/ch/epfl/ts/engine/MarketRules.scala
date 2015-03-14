package ch.epfl.ts.engine

import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{DelOrder, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, Order, Streamable, Transaction}
import ch.epfl.ts.component.fetch.MarketNames

/**
 * represents the cost of placing a bid and market order
 */
case class Commission(limitOrderFee: Double, marketOrderFee: Double)

/**
 * Market Simulator Configuration class. Defines the orders books priority implementation, the matching function and the commission costs of limit and market orders.
 * Extend this class and override its method(s) to customize Market rules for specific markets.
 *
 */
class MarketRules {
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

  def alwaysTrue(a: Double, b: Double) = true

  def matchingFunction(marketId: Long,
                       newOrder: Order,
                       newOrdersBook: PartialOrderBook,
                       bestMatchsBook: PartialOrderBook,
                       send: Streamable => Unit,
                       matchExists: (Double, Double) => Boolean = alwaysTrue,
                       oldTradingPrice: Double,
                       enqueueOrElse: (Order, PartialOrderBook) => Unit): Double = {

    println("MS: got new order: " + newOrder)

    if (bestMatchsBook.isEmpty) {
      println("MS: matching orders book empty")
      enqueueOrElse(newOrder, newOrdersBook)
      oldTradingPrice
    } else {

      val bestMatch = bestMatchsBook.head
      // check if a matching order exists when used with a limit order, if market order: matchExists = true
      if (matchExists(bestMatch.price, newOrder.price)) {

        bestMatchsBook delete bestMatch
        send(DelOrder(bestMatch.oid, bestMatch.uid, newOrder.timestamp, DEF, DEF, 0.0, 0.0))

        // perfect match
        if (bestMatch.volume == newOrder.volume) {
          println("MS: volume match with " + bestMatch)
          println("MS: removing order: " + bestMatch + " from bestMatch orders book.")

          bestMatch match {
            case lbo: LimitBidOrder =>
              send(Transaction(marketId, bestMatch.price, bestMatch.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.uid, bestMatch.oid, newOrder.uid, newOrder.oid))
            case lao: LimitAskOrder =>
              send(Transaction(marketId, bestMatch.price, bestMatch.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, newOrder.uid, newOrder.oid, bestMatch.uid, bestMatch.oid))
            case _ => println("MarketRules: casting error")
          }
        } else if (bestMatch.volume > newOrder.volume) {
          println("MS: matched with " + bestMatch + ", new order volume inferior - cutting matched order.")
          println("MS: removing order: " + bestMatch + " from match orders book. enqueuing same order with " + (bestMatch.volume - newOrder.volume) + " volume.")
          bestMatch match {
            case lbo: LimitBidOrder =>
              bestMatchsBook insert LimitBidOrder(bestMatch.oid, bestMatch.uid, bestMatch.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.volume - newOrder.volume, bestMatch.price)
              send(Transaction(marketId, bestMatch.price, newOrder.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.uid, bestMatch.oid, newOrder.uid, newOrder.oid))
              send(LimitBidOrder(bestMatch.oid, bestMatch.uid, bestMatch.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.volume - newOrder.volume, bestMatch.price))
            case lao: LimitAskOrder =>
              bestMatchsBook insert LimitAskOrder(bestMatch.oid, bestMatch.uid, bestMatch.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.volume - newOrder.volume, bestMatch.price)
              send(Transaction(marketId, bestMatch.price, newOrder.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, newOrder.uid, newOrder.oid, bestMatch.uid, bestMatch.oid))
              send(LimitAskOrder(bestMatch.oid, bestMatch.uid, bestMatch.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.volume - newOrder.volume, bestMatch.price))
            case _ => println("MarketRules: casting error")
          }
        } else {
          println("MS: matched with " + bestMatch + ", new order volume superior - reiterate")
          println("MS: removing order: " + bestMatch + " from match orders book.")

          bestMatch match {
            case lbo: LimitBidOrder =>
              send(Transaction(marketId, bestMatch.price, bestMatch.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, newOrder.uid, newOrder.oid, bestMatch.uid, bestMatch.oid))
              matchingFunction(marketId, LimitBidOrder(newOrder.oid, newOrder.uid, newOrder.timestamp, newOrder.whatC, newOrder.withC, newOrder.volume - bestMatch.volume, bestMatch.price), newOrdersBook, bestMatchsBook, send, matchExists, oldTradingPrice, enqueueOrElse)
            case lao: LimitAskOrder =>
              send(Transaction(marketId, bestMatch.price, bestMatch.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.uid, bestMatch.oid, newOrder.uid, newOrder.oid))
              matchingFunction(marketId, LimitAskOrder(newOrder.oid, newOrder.uid, newOrder.timestamp, newOrder.whatC, newOrder.withC, newOrder.volume - bestMatch.volume, bestMatch.price), newOrdersBook, bestMatchsBook, send, matchExists, oldTradingPrice, enqueueOrElse)
            case mbo: MarketBidOrder =>
              send(Transaction(marketId, bestMatch.price, bestMatch.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, newOrder.uid, newOrder.oid, bestMatch.uid, bestMatch.oid))
              matchingFunction(marketId, MarketBidOrder(newOrder.oid, newOrder.uid, newOrder.timestamp, newOrder.whatC, newOrder.withC, newOrder.volume - bestMatch.volume, newOrder.price), newOrdersBook, bestMatchsBook, send, matchExists, oldTradingPrice, enqueueOrElse)
            case mao: MarketAskOrder =>
              send(Transaction(marketId, bestMatch.price, bestMatch.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.uid, bestMatch.oid, newOrder.uid, newOrder.oid))
              matchingFunction(marketId, MarketAskOrder(newOrder.oid, newOrder.uid, newOrder.timestamp, newOrder.whatC, newOrder.withC, newOrder.volume - bestMatch.volume, newOrder.price), newOrdersBook, bestMatchsBook, send, matchExists, oldTradingPrice, enqueueOrElse)
            case _ => println("MarketRules: casting error")
          }
        }

        // Update price
        bestMatch.price

        // no match found
      } else {
        println("MS: no match found - enqueuing")
        enqueueOrElse(newOrder, newOrdersBook)
        oldTradingPrice
      }
    }
  }
}
