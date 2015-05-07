package ch.epfl.ts.benchmark.marketSimulator

import ch.epfl.ts.data._
import ch.epfl.ts.engine.{MarketRules, OrderBookMarketSimulator}

/**
 * Slightly modified MarketSimulator used for the benchmarks.
 * It manages the case when it receives a LastOrder to notify
 * the end of the benchmark.
 */
class BenchmarkOrderBookMarketSimulator(marketId: Long, rules: MarketRules) extends OrderBookMarketSimulator(marketId, rules) {

  override def receiver = {
    case last: LastOrder =>
      send(FinishedProcessingOrders(book.asks.size, book.bids.size));
      
    case limitBid: LimitBidOrder =>
      val currentPrice = tradingPrices((limitBid.withC, limitBid.whatC))
      val newBidPrice = rules.matchingFunction(
          marketId, limitBid, book.bids, book.asks,
          this.send[Streamable],
          (a, b) => a <= b,
          currentPrice._1,
          (limitBid, bidOrdersBook) => { bidOrdersBook insert limitBid; send(limitBid) })
    tradingPrices((limitBid.withC, limitBid.whatC)) = (newBidPrice, currentPrice._2)
          
    case limitAsk: LimitAskOrder =>
      // TODO: check that the currencies have not been swapped by mistake
      val currentPrice = tradingPrices((limitAsk.withC, limitAsk.whatC))
      val newAskPrice = rules.matchingFunction(
          marketId, limitAsk, book.asks, book.bids,
          this.send[Streamable],
          (a, b) => a >= b,
          currentPrice._2,
          (limitAsk, askOrdersBook) => { askOrdersBook insert limitAsk; send(limitAsk) })
     tradingPrices((limitAsk.withC, limitAsk.whatC)) = (currentPrice._1, newAskPrice)
    
    case marketBid: MarketBidOrder =>
      val currentPrice = tradingPrices((marketBid.withC, marketBid.whatC))
      val newBidPrice = rules.matchingFunction(
          marketId, marketBid, book.bids, book.asks,
          this.send[Streamable],
          (a, b) => true,
          currentPrice._1, 
          (marketBid, bidOrdersBook) => ())
      tradingPrices((marketBid.withC, marketBid.whatC)) = (newBidPrice, currentPrice._2)
    
    case marketAsk: MarketAskOrder =>
      val currentPrice = tradingPrices((marketAsk.withC, marketAsk.whatC))
      val newAskPrice = rules.matchingFunction(
          marketId, marketAsk, book.asks, book.bids,
          this.send[Streamable],
          (a, b) => true,
          currentPrice._2,
          (marketAsk, askOrdersBook) => ())
      tradingPrices((marketAsk.withC, marketAsk.whatC)) = (currentPrice._1, newAskPrice)
    
    case del: DelOrder =>
      send(del)
      book delete del
    
    case t: Transaction =>
      // TODO: how to know which currency of the two was bought? (Which to update, bid or ask price?)
      tradingPrices((t.withC, t.whatC)) = (???, ???)
    
    case _              =>
      println("MS: got unknown")
  }
}
