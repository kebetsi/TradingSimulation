package ch.epfl.ts.engine

import ch.epfl.ts.component.Component
import ch.epfl.ts.data._

class MarketFXSimulator(marketId: Long, rules: MarketFXRules) extends MarketSimulator(marketId, rules) {

  val book = OrderBook(rules.bidsOrdering, rules.asksOrdering)
  /**
   * the current Market Price
   */

  override def receiver = {
    case limitBid: LimitBidOrder =>

    case limitAsk: LimitAskOrder =>

    case marketBid: MarketBidOrder =>
      tradingPrices.get((marketBid.whatC, marketBid.withC)) match {
        // We buy at current ask price
        case Some(t) => rules.matchingFunction(marketId, marketBid, book.bids, book.asks, this.send[Streamable], t._2)
        case None    => // TODO: throw an error of some kind
      }
    case marketAsk: MarketAskOrder =>
      tradingPrices.get((marketAsk.whatC, marketAsk.withC)) match {
        // We sell at current bid price
        case Some(t) => rules.matchingFunction(marketId, marketAsk, book.bids, book.asks, this.send[Streamable], t._1)
        case None    => // TODO: throw an error of some kind
      }
    case q: Quote =>
      tradingPrices((q.whatC, q.withC)) = (q.bid, q.ask)

    case _ =>
      println("MS: got unknown")
  }
}
