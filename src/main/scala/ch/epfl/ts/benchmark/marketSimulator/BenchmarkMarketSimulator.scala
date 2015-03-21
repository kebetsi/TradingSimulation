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
      val currentPrice = tradingPrices((limitBid.withC, limitBid.whatC))
      tradingPrices((limitBid.withC, limitBid.whatC)) = rules.matchingFunction(
          marketId, limitBid, book.bids, book.asks,
          this.send[Streamable],
          (a, b) => a <= b,
          currentPrice,
          (limitBid, bidOrdersBook) => { bidOrdersBook insert limitBid; send(limitBid) })
    
    case limitAsk: LimitAskOrder =>
      // TODO: check that the currencies have not been swapped by mistake
      val currentPrice = tradingPrices((limitAsk.withC, limitAsk.whatC))
      tradingPrices((limitAsk.withC, limitAsk.whatC)) = rules.matchingFunction(
          marketId, limitAsk, book.asks, book.bids,
          this.send[Streamable],
          (a, b) => a >= b,
          currentPrice,
          (limitAsk, askOrdersBook) => { askOrdersBook insert limitAsk; send(limitAsk) })
    
    case marketBid: MarketBidOrder =>
      val currentPrice = tradingPrices((marketBid.withC, marketBid.whatC))
      tradingPrices((marketBid.withC, marketBid.whatC)) = rules.matchingFunction(
          marketId, marketBid, book.bids, book.asks,
          this.send[Streamable],
          (a, b) => true,
          currentPrice, 
          (marketBid, bidOrdersBook) => ())
    
    case marketAsk: MarketAskOrder =>
      // TODO: check that the currencies have not been swapped by mistake
      val currentPrice = tradingPrices((marketAsk.withC, marketAsk.whatC))
      tradingPrices((marketAsk.withC, marketAsk.whatC)) = rules.matchingFunction(
          marketId, marketAsk, book.asks, book.bids,
          this.send[Streamable],
          (a, b) => true,
          currentPrice,
          (marketAsk, askOrdersBook) => ())
    
    case del: DelOrder =>
      send(del)
      book delete del
    
    case t: Transaction =>
      tradingPrices((t.withC, t.whatC)) = t.price
    
    case _              =>
      println("MS: got unknown")
  }
}
