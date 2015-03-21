package ch.epfl.ts.engine

import ch.epfl.ts.component.Component
import ch.epfl.ts.data._


/**
 * Message used to print the books contents (since we use PriotityQueues, it's the heap order)
 */
case class PrintBooks()

class OrderBookMarketSimulator(marketId: Long, rules: MarketRules) extends Component {

  val book = OrderBook(rules.bidsOrdering, rules.asksOrdering)
  /**
   * the price at which the last transaction was executed
   */
  var tradingPrice: Double = 185000.0 // set for SobiTrader when using with finance.csv

  override def receiver = {
    case limitBid: LimitBidOrder =>
      tradingPrice = rules.matchingFunction(marketId, limitBid, book.bids, book.asks, this.send[Streamable], (a, b) => a <= b, tradingPrice, (limitBid, bidOrdersBook) => {
        bidOrdersBook insert limitBid; send(limitBid); println("MS: order enqueued")
      })
    case limitAsk: LimitAskOrder =>
      tradingPrice = rules.matchingFunction(marketId, limitAsk, book.asks, book.bids, this.send[Streamable], (a, b) => a >= b, tradingPrice, (limitAsk, askOrdersBook) => {
        askOrdersBook insert limitAsk; send(limitAsk); println("MS: order enqueued")
      })
    case marketBid: MarketBidOrder =>
      tradingPrice = rules.matchingFunction(marketId, marketBid, book.bids, book.asks, this.send[Streamable], (a, b) => true, tradingPrice, (marketBid, bidOrdersBook) => println("MS: market order discarded"))
    case marketAsk: MarketAskOrder =>
      tradingPrice = rules.matchingFunction(marketId, marketAsk, book.asks, book.bids, this.send[Streamable], (a, b) => true, tradingPrice, (marketAsk, askOrdersBook) => println("MS: market order discarded"))
    case del: DelOrder =>
      println("MS: got Delete: " + del)
      send(del)
      book delete del
    case t: Transaction =>
      tradingPrice = t.price
    case q : Quote =>
    //Using MarketFXSimulator
    case PrintBooks =>
      // print shows heap order (binary tree)
      println("Ask Orders Book: " + book.bids)
      println("Bid Orders Book: " + book.asks)
    case _ =>
      println("MS: got unknown")
  }
}
