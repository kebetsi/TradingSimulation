package ch.epfl.ts.benchmark.marketSimulator

import ch.epfl.ts.engine.MarketRules
import ch.epfl.ts.data.{ Order, Transaction, MarketAskOrder, MarketBidOrder, LimitAskOrder, LimitBidOrder, DelOrder }
import scala.collection.mutable.TreeSet
import ch.epfl.ts.data.Streamable
import ch.epfl.ts.data.Currency._

class BenchmarkMarketRules extends MarketRules {

  override def matchingFunction(marketId: Long, newOrder: Order, newOrdersBook: TreeSet[Order], bestMatchsBook: TreeSet[Order], send: Streamable => Unit, matchExists: (Double, Double) => Boolean = alwaysTrue, oldTradingPrice: Double, enqueueOrElse: (Order, TreeSet[Order]) => Unit): Double = {
    //    println("MS: got new order: " + newOrder)

    if (bestMatchsBook.isEmpty) {
      //      println("MS: matching orders book empty")
      enqueueOrElse(newOrder, newOrdersBook)
      return oldTradingPrice
    } else {

      val bestMatch = bestMatchsBook.head
      // check if a matching order exists when used with a limit order, if market order: matchExists = true
      if (matchExists(bestMatch.price, newOrder.price)) {

        // perfect match
        if (bestMatch.volume == newOrder.volume) {
          //          println("MS: volume match with " + bestMatch)
          // send transaction
          if (bestMatch.isInstanceOf[LimitBidOrder]) {
            send(Transaction(marketId, bestMatch.price, bestMatch.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.uid, bestMatch.oid, newOrder.uid, newOrder.oid))
          } else if (bestMatch.isInstanceOf[LimitAskOrder]) {
            send(Transaction(marketId, bestMatch.price, bestMatch.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, newOrder.uid, newOrder.oid, bestMatch.uid, bestMatch.oid))
          } else {
            //            println("error")
          }
          // remove matched order
          //          println("MS: removing order: " + bestMatch + " from bestMatch orders book.")
          bestMatchsBook -= bestMatch
          // send diff
          send(DelOrder(bestMatch.oid, bestMatch.uid, newOrder.timestamp, DEF, DEF, 0.0, 0.0))
          // update price
          return bestMatch.price

        } else if (bestMatch.volume > newOrder.volume) {
          //          println("MS: matched with " + bestMatch + ", new order volume inferior - cutting matched order.")
          // remove matched order and reinput it with updated volume
          //          println("MS: removing order: " + bestMatch + " from match orders book. enqueuing same order with " + (bestMatch.volume - newOrder.volume) + " volume.")
          bestMatchsBook -= bestMatch
          // send diff
          send(DelOrder(bestMatch.oid, bestMatch.uid, newOrder.timestamp, DEF, DEF, 0.0, 0.0))
          if (bestMatch.isInstanceOf[LimitBidOrder]) {
            send(Transaction(marketId, bestMatch.price, newOrder.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.uid, bestMatch.oid, newOrder.uid, newOrder.oid))
            bestMatchsBook += LimitBidOrder(bestMatch.oid, bestMatch.uid, bestMatch.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.volume - newOrder.volume, bestMatch.price)
            // send diff
            send(LimitBidOrder(bestMatch.oid, bestMatch.uid, bestMatch.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.volume - newOrder.volume, bestMatch.price))
          } else if (bestMatch.isInstanceOf[LimitAskOrder]) {
            send(Transaction(marketId, bestMatch.price, newOrder.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, newOrder.uid, newOrder.oid, bestMatch.uid, bestMatch.oid))
            bestMatchsBook += LimitAskOrder(bestMatch.oid, bestMatch.uid, bestMatch.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.volume - newOrder.volume, bestMatch.price)
            // send diff
            send(LimitAskOrder(bestMatch.oid, bestMatch.uid, bestMatch.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.volume - newOrder.volume, bestMatch.price))
          } else {
            // error
            //            println("MarketRules: casting error")
          }

          // update price
          return bestMatch.price
        } else {
          //          println("MS: matched with " + bestMatch + ", new order volume superior - reiterate")
          // remove matched ask order
          //          println("MS: removing order: " + bestMatch + " from match orders book.")
          bestMatchsBook -= bestMatch
          // send diff
          send(DelOrder(bestMatch.uid, bestMatch.oid, bestMatch.timestamp, DEF, DEF, 0.0, 0.0))
          // call handleNewOrder on bid with updated volume
          if (newOrder.isInstanceOf[LimitBidOrder]) {
            // create transaction
            send(Transaction(marketId, bestMatch.price, bestMatch.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, newOrder.uid, newOrder.oid, bestMatch.uid, bestMatch.oid))
            matchingFunction(marketId, LimitBidOrder(newOrder.oid, newOrder.uid, newOrder.timestamp, newOrder.whatC, newOrder.withC, newOrder.volume - bestMatch.volume, bestMatch.price), newOrdersBook.asInstanceOf[TreeSet[Order]], bestMatchsBook, send, matchExists, oldTradingPrice, enqueueOrElse)
          } else if (newOrder.isInstanceOf[LimitAskOrder]) {
            // create transaction
            send(Transaction(marketId, bestMatch.price, bestMatch.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.uid, bestMatch.oid, newOrder.uid, newOrder.oid))
            matchingFunction(marketId, LimitAskOrder(newOrder.oid, newOrder.uid, newOrder.timestamp, newOrder.whatC, newOrder.withC, newOrder.volume - bestMatch.volume, bestMatch.price), newOrdersBook.asInstanceOf[TreeSet[Order]], bestMatchsBook, send, matchExists, oldTradingPrice, enqueueOrElse)
          } else if (newOrder.isInstanceOf[MarketBidOrder]) {
            // create transaction
            send(Transaction(marketId, bestMatch.price, bestMatch.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, newOrder.uid, newOrder.oid, bestMatch.uid, bestMatch.oid))
            matchingFunction(marketId, MarketBidOrder(newOrder.oid, newOrder.uid, newOrder.timestamp, newOrder.whatC, newOrder.withC, newOrder.volume - bestMatch.volume, newOrder.price), newOrdersBook.asInstanceOf[TreeSet[Order]], bestMatchsBook, send, matchExists, oldTradingPrice, enqueueOrElse)
          } else if (newOrder.isInstanceOf[MarketAskOrder]) {
            // create transaction
            send(Transaction(marketId, bestMatch.price, bestMatch.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.uid, bestMatch.oid, newOrder.uid, newOrder.oid))
            matchingFunction(marketId, MarketAskOrder(newOrder.oid, newOrder.uid, newOrder.timestamp, newOrder.whatC, newOrder.withC, newOrder.volume - bestMatch.volume, newOrder.price), newOrdersBook.asInstanceOf[TreeSet[Order]], bestMatchsBook, send, matchExists, oldTradingPrice, enqueueOrElse)
          } else {
            //            println("MarketRules: casting error")
          }
          // update price
          return bestMatch.price
        }
        // no match found
      } else {
        //        println("MS: no match found")
        // enqueue
        enqueueOrElse(newOrder, newOrdersBook.asInstanceOf[TreeSet[Order]])
        return oldTradingPrice
      }
    }
  }
}