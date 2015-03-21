package ch.epfl.ts.engine

import scala.collection.mutable.{ HashMap => MHashMap }
import ch.epfl.ts.component.Component
import ch.epfl.ts.data._


/**
 * Message used to print the books contents (since we use PriotityQueues, it's the heap order)
 */
case class PrintBooks()


class OrderBookMarketSimulator(marketId: Long, rules: MarketRules) extends MarketSimulator {
  
  // TODO: need to set initial trading price?
  //var tradingPrice: Double = 185000.0 // set for SobiTrader when using with finance.csv
  
  val book = OrderBook(rules.bidsOrdering, rules.asksOrdering)
  
  override def receiver = {
    case limitBid: LimitBidOrder =>
      val currentPrice = tradingPrices((limitBid.withC, limitBid.whatC))
      tradingPrices((limitBid.withC, limitBid.whatC)) =
          rules.matchingFunction(
              marketId, limitBid, book.bids, book.asks,
              this.send[Streamable],
              (a, b) => a <= b, currentPrice,
              (limitBid, bidOrdersBook) => {
                bidOrdersBook insert limitBid;
                send(limitBid);
                println("MS: order enqueued")
              })
              
    case limitAsk: LimitAskOrder =>
      // TODO: check currencies are not being swapped by mistake
      val currentPrice = tradingPrices((limitAsk.withC, limitAsk.whatC))
      tradingPrices((limitAsk.withC, limitAsk.whatC)) = rules.matchingFunction(
          marketId, limitAsk, book.asks, book.bids,
          this.send[Streamable],
          (a, b) => a >= b, currentPrice,
          (limitAsk, askOrdersBook) => {
            askOrdersBook insert limitAsk;
            send(limitAsk);
            println("MS: order enqueued")
          })
      
    case marketBid: MarketBidOrder =>
      val currentPrice = tradingPrices((marketBid.withC, marketBid.whatC))
      tradingPrices((marketBid.withC, marketBid.whatC)) = rules.matchingFunction(
          marketId, marketBid, book.bids, book.asks,
          this.send[Streamable],
          (a, b) => true,
          currentPrice,
          (marketBid, bidOrdersBook) => println("MS: market order discarded"))
    
    case marketAsk: MarketAskOrder =>
      // TODO: check currencies haven't been swapped here by mistake
      val currentPrice = tradingPrices((marketAsk.withC, marketAsk.whatC))
      tradingPrices((marketAsk.withC, marketAsk.whatC)) = rules.matchingFunction(
          marketId, marketAsk, book.asks, book.bids,
          this.send[Streamable],
          (a, b) => true,
          currentPrice,
          (marketAsk, askOrdersBook) => println("MS: market order discarded"))
    
    case del: DelOrder =>
      println("MS: got Delete: " + del)
      send(del)
      book delete del
   
    case t: Transaction =>
      tradingPrices((t.withC, t.whatC)) = t.price
   
    case PrintBooks =>
      // print shows heap order (binary tree)
      println("Ask Orders Book: " + book.bids)
      println("Bid Orders Book: " + book.asks)
    
    case _ =>
      println("MS: got unknown")
  }
}
