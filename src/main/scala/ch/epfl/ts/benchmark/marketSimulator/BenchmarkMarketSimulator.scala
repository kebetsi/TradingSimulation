package ch.epfl.ts.benchmark.marketSimulator

import ch.epfl.ts.data._
import ch.epfl.ts.engine.{MarketRules, OrderBookMarketSimulator}

/**
 * Slightly modified MarketSimulator used for the benchmarks.
 * It manages the case when it receives a LastOrder to notify
 * the end of the benchmark.
 */
class BenchmarkMarketSimulator(marketId: Long, rules: MarketRules) extends OrderBookMarketSimulator(marketId, rules) {


  override def receiver = {
    case last: LastOrder =>
      send(FinishedProcessingOrders(book.asks.size, book.bids.size));
    case limitBid: LimitBidOrder =>
      tradingPrice = rules.matchingFunction(marketId, limitBid, book.bids, book.asks, this.send[Streamable], (a, b) => a <= b, tradingPrice, (limitBid, bidOrdersBook) => { bidOrdersBook insert limitBid; send(limitBid) })
    case limitAsk: LimitAskOrder =>
      tradingPrice = rules.matchingFunction(marketId, limitAsk, book.asks, book.bids, this.send[Streamable], (a, b) => a >= b, tradingPrice, (limitAsk, askOrdersBook) => { askOrdersBook insert limitAsk; send(limitAsk) })
    case marketBid: MarketBidOrder =>
      tradingPrice = rules.matchingFunction(marketId, marketBid, book.bids, book.asks, this.send[Streamable], (a, b) => true, tradingPrice, (marketBid, bidOrdersBook) => ())
    case marketAsk: MarketAskOrder =>
      tradingPrice = rules.matchingFunction(marketId, marketAsk, book.asks, book.bids, this.send[Streamable], (a, b) => true, tradingPrice, (marketAsk, askOrdersBook) => ())
    case del: DelOrder =>
      send(del)
      book delete del
    case t: Transaction =>
      tradingPrice = t.price
    case _              =>
      println("MS: got unknown")
  }
}
