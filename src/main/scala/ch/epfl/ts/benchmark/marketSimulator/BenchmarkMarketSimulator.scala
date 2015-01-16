package ch.epfl.ts.benchmark.marketSimulator

import ch.epfl.ts.engine.{ MarketSimulator, MarketRules }
import ch.epfl.ts.data.{ DelOrder, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, Order, Transaction }
import ch.epfl.ts.data.Streamable

class BenchmarkMarketSimulator(marketId: Long, rules: MarketRules) extends MarketSimulator(marketId, rules) {

//  var ordersCount: Int = 0

  override def receiver = {
    case last: LastOrder => send(FinishedProcessingOrders(book.asks.size, book.bids.size));

    case limitBid: LimitBidOrder => {
      tradingPrice = rules.matchingFunction(marketId, limitBid.asInstanceOf[Order], book.bids, book.asks, this.send[Streamable], (a, b) => a <= b, tradingPrice, (limitBid, bidOrdersBook) => { bidOrdersBook insert limitBid; send(limitBid) }) //; println("MS: order enqueued") })
//      ordersCount = ordersCount + 1
    }
    case limitAsk: LimitAskOrder => {
      tradingPrice = rules.matchingFunction(marketId, limitAsk.asInstanceOf[Order], book.asks, book.bids, this.send[Streamable], (a, b) => a >= b, tradingPrice, (limitAsk, askOrdersBook) => { askOrdersBook insert limitAsk; send(limitAsk) }) //; println("MS: order enqueued") })
//      ordersCount = ordersCount + 1
    }
    case marketBid: MarketBidOrder => {
      tradingPrice = rules.matchingFunction(marketId, marketBid.asInstanceOf[Order], book.bids, book.asks, this.send[Streamable], (a, b) => true, tradingPrice, (marketBid, bidOrdersBook) => ()) //(println("MS: market order discarded")))
//      ordersCount = ordersCount + 1
    }
    case marketAsk: MarketAskOrder => {
      tradingPrice = rules.matchingFunction(marketId, marketAsk.asInstanceOf[Order], book.asks, book.bids, this.send[Streamable], (a, b) => true, tradingPrice, (marketAsk, askOrdersBook) => ()) //(println("MS: market order discarded")))
//      ordersCount = ordersCount + 1
    }

    case del: DelOrder =>
      //      println("MS: got Delete: " + del)
      send(del)
      book delete del

    case t: Transaction => tradingPrice = t.price

    case _              => println("MS: got unknown")
  }
}
