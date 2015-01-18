package ch.epfl.ts.engine

import ch.epfl.ts.component.Component
import ch.epfl.ts.data._

import scala.collection.mutable.{TreeSet => MTreeSet}

case class PrintBooks()

class MarketSimulator(marketId: Long, rules: MarketRules) extends Component {

  /**
   * the price at which the last transaction was executed
   */
  var tradingPrice: Double = 185000.0 // set for SobiTrader when using with finance.csv

  val book = OrderBook(rules.bidsOrdering, rules.asksOrdering)

  override def receiver = {
    case limitBid: LimitBidOrder =>
      tradingPrice = rules.matchingFunction(marketId, limitBid.asInstanceOf[Order], book.bids, book.asks, this.send[Streamable], (a, b) => a <= b, tradingPrice, (limitBid, bidOrdersBook) => { bidOrdersBook insert limitBid; send(limitBid); println("MS: order enqueued") })
    case limitAsk: LimitAskOrder =>
      tradingPrice = rules.matchingFunction(marketId, limitAsk.asInstanceOf[Order], book.asks, book.bids, this.send[Streamable], (a, b) => a >= b, tradingPrice, (limitAsk, askOrdersBook) => { askOrdersBook insert limitAsk; send(limitAsk); println("MS: order enqueued") })
    case marketBid: MarketBidOrder =>
      tradingPrice = rules.matchingFunction(marketId, marketBid.asInstanceOf[Order], book.bids, book.asks, this.send[Streamable], (a, b) => true, tradingPrice, (marketBid, bidOrdersBook) => println("MS: market order discarded"))
    case marketAsk: MarketAskOrder =>
      tradingPrice = rules.matchingFunction(marketId, marketAsk.asInstanceOf[Order], book.asks, book.bids, this.send[Streamable], (a, b) => true, tradingPrice, (marketAsk, askOrdersBook) => println("MS: market order discarded"))
    case del: DelOrder =>
      println("MS: got Delete: " + del)
      send(del)
      book delete del
    case t: Transaction =>
      tradingPrice = t.price
    // for now we simply add them without trying to match - need to be optimized, first batch loads and simply adds, then smaller batches try matching
    case lla: LiveLimitAskOrder =>
      book insertAskOrder LimitAskOrder(lla.oid, lla.uid, lla.timestamp, lla.whatC, lla.withC, lla.volume, lla.price)
    case llb: LiveLimitBidOrder =>
      book insertBidOrder LimitBidOrder(llb.oid, llb.uid, llb.timestamp, llb.whatC, llb.withC, llb.volume, llb.price)
     case PrintBooks =>
      // print shows heap order (binary tree)
      println("Ask Orders Book: " + book.bids)
      println("Bid Orders Book: " + book.asks)
    case _ =>
      println("MS: got unknown")
  }
}
