package ch.epfl.ts.engine

import scala.collection.mutable.{ HashMap => MHashMap }
import ch.epfl.ts.component.Component
import ch.epfl.ts.data._


/**
 * Message used to print the books contents (since we use PriotityQueues, it's the heap order)
 */
case class PrintBooks()


class OrderBookMarketSimulator(marketId: Long, rules: MarketRules) extends MarketSimulator(marketId, rules) {
  
  override def receiver = {
    case limitBid: LimitBidOrder =>
      val currentPrice = tradingPrices((limitBid.withC, limitBid.whatC))
      val newBidPrice = rules.matchingFunction(
              marketId, limitBid, book.bids, book.asks,
              this.send[Streamable],
              (a, b) => a <= b, currentPrice._1,
              (limitBid, bidOrdersBook) => {
                bidOrdersBook insert limitBid;
                send(limitBid);
                println("MS: order enqueued")
              })
      tradingPrices((limitBid.withC, limitBid.whatC)) = (newBidPrice, currentPrice._2)
      
    case limitAsk: LimitAskOrder =>
      val currentPrice = tradingPrices((limitAsk.withC, limitAsk.whatC))
      val newAskPrice = rules.matchingFunction(
          marketId, limitAsk, book.asks, book.bids,
          this.send[Streamable],
          (a, b) => a >= b, currentPrice._2,
          (limitAsk, askOrdersBook) => {
            askOrdersBook insert limitAsk;
            send(limitAsk);
            println("MS: order enqueued")
          })
      tradingPrices((limitAsk.withC, limitAsk.whatC)) = (currentPrice._1, newAskPrice)
      
    case marketBid: MarketBidOrder =>
      val currentPrice = tradingPrices((marketBid.withC, marketBid.whatC))
      val newBidPrice = rules.matchingFunction(
          marketId, marketBid, book.bids, book.asks,
          this.send[Streamable],
          (a, b) => true,
          currentPrice._1,
          (marketBid, bidOrdersBook) => println("MS: market order discarded"))
      tradingPrices((marketBid.withC, marketBid.whatC)) = (newBidPrice, currentPrice._2)
    
    case marketAsk: MarketAskOrder =>
      // TODO: check currencies haven't been swapped here by mistake
      val currentPrice = tradingPrices((marketAsk.withC, marketAsk.whatC))
      val newAskPrice = rules.matchingFunction(
          marketId, marketAsk, book.asks, book.bids,
          this.send[Streamable],
          (a, b) => true,
          currentPrice._2,
          (marketAsk, askOrdersBook) => println("MS: market order discarded"))
      tradingPrices((marketAsk.withC, marketAsk.whatC)) = (currentPrice._1, newAskPrice)
    
    case del: DelOrder =>
      println("MS: got Delete: " + del)
      send(del)
      book delete del
   
    case t: Transaction =>
      // TODO: how to know which currency of the two was bought? (Which to update, bid or ask price?)
      tradingPrices((t.withC, t.whatC)) = (???, ???)
   
    case PrintBooks =>
      // print shows heap order (binary tree)
      println("Ask Orders Book: " + book.bids)
      println("Bid Orders Book: " + book.asks)
    
    case _ =>
      println("MS: got unknown")
  }
}
