package ch.epfl.ts.benchmark.marketSimulator

import ch.epfl.ts.engine.{PartialOrderBook, MarketRules}
import ch.epfl.ts.data.{ Order, Transaction, MarketAskOrder, MarketBidOrder, LimitAskOrder, LimitBidOrder, DelOrder }
import scala.collection.mutable.TreeSet
import ch.epfl.ts.data.Streamable
import ch.epfl.ts.data.Currency._

class BenchmarkMarketRules extends MarketRules {

  override def matchingFunction(marketId:         Long,
                                newOrder:         Order,
                                newOrdersBook:    PartialOrderBook,
                                bestMatchsBook:   PartialOrderBook,
                                send:             Streamable => Unit,
                                matchExists:      (Double, Double) => Boolean = alwaysTrue,
                                oldTradingPrice:  Double,
                                enqueueOrElse:    (Order, PartialOrderBook) => Unit): Double = {
    if (bestMatchsBook.isEmpty) {
      println("empty")
      enqueueOrElse(newOrder, newOrdersBook)
      oldTradingPrice
    } else {
      val bestMatch = bestMatchsBook.head

      // check if a matching order exists when used with a limit order, if market order: matchExists = true
      if (matchExists(bestMatch.price, newOrder.price)) {

        bestMatchsBook delete bestMatch
        send(DelOrder(bestMatch.oid, bestMatch.uid, newOrder.timestamp, DEF, DEF, 0.0, 0.0))

        if (bestMatch.volume == newOrder.volume) {
          println("matched, =")
          bestMatch match {
            case lbo: LimitBidOrder =>
              send(Transaction(marketId, bestMatch.price, bestMatch.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.uid, bestMatch.oid, newOrder.uid, newOrder.oid))
            case lao: LimitAskOrder =>
              send(Transaction(marketId, bestMatch.price, bestMatch.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, newOrder.uid, newOrder.oid, bestMatch.uid, bestMatch.oid))
            case _ =>
          }
        } else if (bestMatch.volume > newOrder.volume) {
          println("matched, >")
          bestMatch match {
            case lbo: LimitBidOrder =>
              bestMatchsBook insert LimitBidOrder(bestMatch.oid, bestMatch.uid, bestMatch.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.volume - newOrder.volume, bestMatch.price)
              send(Transaction(marketId, bestMatch.price, newOrder.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.uid, bestMatch.oid, newOrder.uid, newOrder.oid))
              send(LimitBidOrder(bestMatch.oid, bestMatch.uid, bestMatch.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.volume - newOrder.volume, bestMatch.price))
            case lao: LimitAskOrder =>
              bestMatchsBook insert LimitAskOrder(bestMatch.oid, bestMatch.uid, bestMatch.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.volume - newOrder.volume, bestMatch.price)
              send(Transaction(marketId, bestMatch.price, newOrder.volume, newOrder.timestamp, bestMatch.whatC, bestMatch.withC, newOrder.uid, newOrder.oid, bestMatch.uid, bestMatch.oid))
              send(LimitAskOrder(bestMatch.oid, bestMatch.uid, bestMatch.timestamp, bestMatch.whatC, bestMatch.withC, bestMatch.volume - newOrder.volume, bestMatch.price))
            case _ =>
          }
        } else {
          println("matched, <")
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
            case _ =>
          }
        }
        bestMatch.price
      } else {
        println("unmatched")
        enqueueOrElse(newOrder, newOrdersBook)
        oldTradingPrice
      }
    }
  }
}