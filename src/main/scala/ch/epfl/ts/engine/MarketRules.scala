package ch.epfl.ts.engine

import scala.collection.mutable.TreeSet
import ch.epfl.ts.data.Transaction

object MarketRules {
  val noCommission = Commission(0, 0)

  def basicMatchingFunction[A <: EngineOrder, B <: EngineOrder](newOrder: A, newOrdersBook: TreeSet[A], bestMatchsBook: TreeSet[B], send: Transaction => Unit, f: (Double, Double) => Boolean, oldTradingPrice: Double, enqueueOrElse: (EngineOrder, TreeSet[EngineOrder]) => Unit): Double = {
    println("Market: got new order: " + newOrder)

    if (bestMatchsBook.isEmpty) {
      println("Market: matching orders book empty")
      enqueueOrElse(newOrder, newOrdersBook.asInstanceOf[TreeSet[EngineOrder]])
      return oldTradingPrice
    } else {

      val bestMatch = bestMatchsBook.head
      // check if a matching order exists when used with a limit order, if market order: f = true
      if (f(bestMatch.price, newOrder.price)) {

        // perfect match
        if (bestMatch.quantity == newOrder.quantity) {
          println("Market: volume match with " + bestMatch)
          // send transaction
          send(new Transaction(newOrder.price, newOrder.quantity, newOrder.timestamp, newOrder.whatC, newOrder.uid, newOrder.oid, bestMatch.uid, bestMatch.oid))
          // remove matched order
          println("removing order: " + bestMatchsBook.head + " from bestMatch orders book.")
          bestMatchsBook -= bestMatchsBook.head
          // update price
          return newOrder.price

        } else if (bestMatch.quantity > newOrder.quantity) {
          println("Market: new order volume inferior - cutting matched order")
          // send transaction
          send(new Transaction(newOrder.price, newOrder.quantity, newOrder.timestamp, newOrder.whatC, newOrder.uid, newOrder.oid, bestMatch.uid, bestMatch.oid))
          // remove matched order and reinput it with updated volume
          println("removing order: " + bestMatchsBook.head + " from match orders book. enqueuing same order with " + (bestMatch.quantity - newOrder.quantity) + " volume.")
          bestMatchsBook -= bestMatchsBook.head
          if (bestMatch.isInstanceOf[LimitBidOrder]) {
            bestMatchsBook += LimitBidOrder(bestMatch.uid, bestMatch.oid, bestMatch.timestamp, bestMatch.whatC, bestMatch.price, bestMatch.quantity - newOrder.quantity, bestMatch.withC).asInstanceOf[B]
          } else if (bestMatch.isInstanceOf[LimitAskOrder]) {
            bestMatchsBook += LimitAskOrder(bestMatch.uid, bestMatch.oid, bestMatch.timestamp, bestMatch.whatC, bestMatch.price, bestMatch.quantity - newOrder.quantity, bestMatch.withC).asInstanceOf[B]
          } else {
            // error
            println("MarketRules: casting error")
          }
          // update price
          return newOrder.price
        } else {
          println("Market: new order volume superior - reiterate ")
          // create transaction
          send(new Transaction(newOrder.price, bestMatch.quantity, newOrder.timestamp, newOrder.whatC, newOrder.uid, newOrder.oid, bestMatch.uid, bestMatch.oid))
          // remove matched ask order
          println("removing order: " + bestMatchsBook.head + " from match orders book.")
          bestMatchsBook -= bestMatchsBook.head
          // call handleNewOrder on bid with updated volume
          if (newOrder.isInstanceOf[LimitBidOrder]) {
            basicMatchingFunction(new LimitBidOrder(newOrder.uid, newOrder.oid, newOrder.timestamp, newOrder.whatC, newOrder.price, newOrder.quantity - bestMatch.quantity, newOrder.withC), newOrdersBook.asInstanceOf[TreeSet[EngineOrder]], bestMatchsBook, send, f, oldTradingPrice, enqueueOrElse)
          } else if (newOrder.isInstanceOf[LimitAskOrder]) {
            basicMatchingFunction(new LimitAskOrder(newOrder.uid, newOrder.oid, newOrder.timestamp, newOrder.whatC, newOrder.price, newOrder.quantity - bestMatch.quantity, newOrder.withC), newOrdersBook.asInstanceOf[TreeSet[EngineOrder]], bestMatchsBook, send, f, oldTradingPrice, enqueueOrElse)
          } else if (newOrder.isInstanceOf[MarketBidOrder]) {
            basicMatchingFunction(new MarketBidOrder(newOrder.uid, newOrder.oid, newOrder.timestamp, newOrder.whatC, newOrder.price, newOrder.quantity - bestMatch.quantity, newOrder.withC), newOrdersBook.asInstanceOf[TreeSet[EngineOrder]], bestMatchsBook, send, f, oldTradingPrice, enqueueOrElse)
          } else if (newOrder.isInstanceOf[MarketAskOrder]) {
            basicMatchingFunction(new MarketAskOrder(newOrder.uid, newOrder.oid, newOrder.timestamp, newOrder.whatC, newOrder.price, newOrder.quantity - bestMatch.quantity, newOrder.withC), newOrdersBook.asInstanceOf[TreeSet[EngineOrder]], bestMatchsBook, send, f, oldTradingPrice, enqueueOrElse)
          } else {
            println("MarketRules: casting error")
          }
          // update price
          return newOrder.price
        }
        // no match found
      } else {
        println("Market: no match found - enqueuing")
        // enqueue
        enqueueOrElse(newOrder, newOrdersBook.asInstanceOf[TreeSet[EngineOrder]])
        return oldTradingPrice
      }
    }
  }
}

/**
 * represents the cost of placing a bid and market order
 */
case class Commission(limitOrder: Double, marketOrder: Double)

/**
 * @param bidOrdersBookPriority
 *  Ordering used by the bid orders book
 * @param askOrdersBookPriority
 *  Ordering used by the ask orders book
 * @param matchingFunction
 *
 */
class MarketRules(bidOrdersBookPriority: Ordering[LimitBidOrder], askOrdersBookPriority: Ordering[LimitAskOrder], matchingFunction: (EngineOrder, TreeSet[EngineOrder], TreeSet[EngineOrder], Transaction => Unit, (Double, Double) => Boolean, Double, (EngineOrder, TreeSet[EngineOrder]) => Unit) => Double = MarketRules.basicMatchingFunction, commission: Commission = MarketRules.noCommission) {

}