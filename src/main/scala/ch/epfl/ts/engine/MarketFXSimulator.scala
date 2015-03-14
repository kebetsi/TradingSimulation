package ch.epfl.ts.engine

import ch.epfl.ts.component.Component
import ch.epfl.ts.data._

class MarketFXSimulator(marketId: Long, rules: MarketFXRules) extends Component {

  val book = OrderBook(rules.bidsOrdering, rules.asksOrdering)
  /**
   * the current Market Price
   */

  // xxx/yyy -> (bidPrice,askPrice)  
  var tradingPrices: Map[String, (Double, Double)] = Map()

  override def receiver = {
    case limitBid: LimitBidOrder =>

    case limitAsk: LimitAskOrder =>

    case marketBid: MarketBidOrder =>
      tradingPrices.get(marketBid.whatC + "/" + marketBid.withC) match {
        //we buy at ask 
        case Some(t) => rules.matchingFunction(marketId, marketBid, book.bids, book.asks, this.send[Streamable], t._2)
        case None    =>
      }
    case marketAsk: MarketAskOrder =>
      tradingPrices.get(marketAsk.whatC + "/" + marketAsk.withC) match {
        //we sell at bid 
        case Some(t) => rules.matchingFunction(marketId, marketAsk, book.bids, book.asks, this.send[Streamable], t._1)
        case None    =>
      }
    case q: Quote =>
      tradingPrices += q.whatC + "/" + q.withC -> (q.bid, q.ask)

    case _ =>
      println("MS: got unknown")
  }
}
