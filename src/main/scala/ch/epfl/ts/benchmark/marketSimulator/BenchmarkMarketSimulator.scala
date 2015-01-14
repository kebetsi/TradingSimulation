package ch.epfl.ts.benchmark.marketSimulator

import ch.epfl.ts.engine.{ MarketSimulator, MarketRules }
import ch.epfl.ts.data.{ DelOrder, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, Order, Transaction }
import scala.collection.mutable.TreeSet
import ch.epfl.ts.data.Streamable
import ch.epfl.ts.component.StopSignal

class BenchmarkMarketSimulator(marketId: Long, rules: MarketRules) extends MarketSimulator(marketId, rules) {

//  var ordersCount: Int = 0

  override def receiver = {
    case last: LastOrder => send(FinishedProcessingOrders()); 

    case limitBid: LimitBidOrder => {
      tradingPrice = rules.matchingFunction(marketId, limitBid.asInstanceOf[Order], bidOrdersBook.asInstanceOf[TreeSet[Order]], askOrdersBook.asInstanceOf[TreeSet[Order]], this.send[Streamable], (a, b) => a <= b, tradingPrice, (limitBid, bidOrdersBook) => { bidOrdersBook += limitBid; send(limitBid) }) //; println("MS: order enqueued") })
//      ordersCount = ordersCount + 1
    }
    case limitAsk: LimitAskOrder => {
      tradingPrice = rules.matchingFunction(marketId, limitAsk.asInstanceOf[Order], askOrdersBook.asInstanceOf[TreeSet[Order]], bidOrdersBook.asInstanceOf[TreeSet[Order]], this.send[Streamable], (a, b) => a >= b, tradingPrice, (limitAsk, askOrdersBook) => { askOrdersBook += limitAsk; send(limitAsk) }) //; println("MS: order enqueued") })
//      ordersCount = ordersCount + 1
    }
    case marketBid: MarketBidOrder => {
      tradingPrice = rules.matchingFunction(marketId, marketBid.asInstanceOf[Order], bidOrdersBook.asInstanceOf[TreeSet[Order]], askOrdersBook.asInstanceOf[TreeSet[Order]], this.send[Streamable], (a, b) => true, tradingPrice, (marketBid, bidOrdersBook) => ()) //(println("MS: market order discarded")))
//      ordersCount = ordersCount + 1
    }
    case marketAsk: MarketAskOrder => {
      tradingPrice = rules.matchingFunction(marketId, marketAsk.asInstanceOf[Order], askOrdersBook.asInstanceOf[TreeSet[Order]], bidOrdersBook.asInstanceOf[TreeSet[Order]], this.send[Streamable], (a, b) => true, tradingPrice, (marketAsk, askOrdersBook) => ()) //(println("MS: market order discarded")))
//      ordersCount = ordersCount + 1
    }

    case del: DelOrder => {
      //      println("MS: got Delete: " + del)
      send(del)
      // look in bids
      bidOrdersBook.find { _.oid == del.oid } map { elem =>
        //        println("MS: order deleted from Bids")
        bidOrdersBook -= elem
      } getOrElse {
        // look in asks
        askOrdersBook.find { _.oid == del.oid } map { elem =>
          //          println("MS: order deleted from Asks")
          askOrdersBook -= elem
        }
      }
//      ordersCount = ordersCount + 1
    }

    case t: Transaction => tradingPrice = t.price

    case _              => println("MS: got unknown")
  }
}
