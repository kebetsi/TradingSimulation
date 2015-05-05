package ch.epfl.ts.engine

import ch.epfl.ts.data.LimitAskOrder
import ch.epfl.ts.data.LimitBidOrder
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.data.Quote
import ch.epfl.ts.data.Streamable
import akka.actor.ActorLogging

class MarketFXSimulator(marketId: Long, rules: ForexMarketRules) extends MarketSimulator(marketId, rules) with ActorLogging {

  override def receiver = {
    case limitBid: LimitBidOrder =>
    // TODO

    case limitAsk: LimitAskOrder =>
    // TODO

    case marketBid: MarketBidOrder =>
      log.debug("MarketFXSimulator : received a bidOrder")
      tradingPrices.get((marketBid.whatC, marketBid.withC)) match {
        // We buy at current ask price
        case Some(t) => rules.matchingFunction(marketId, marketBid, book.bids, book.asks, this.send[Streamable], t._2)
        case None    => // TODO: throw an error of some kind
      }
    case marketAsk: MarketAskOrder =>
      log.debug("MarketFXSimulator : received an askOrder")
      tradingPrices.get((marketAsk.whatC, marketAsk.withC)) match {
        // We sell at current bid price
        case Some(t) => rules.matchingFunction(marketId, marketAsk, book.bids, book.asks, this.send[Streamable], t._1)
        case None    => // TODO: throw an error of some kind
      }
    //TODO Forward quote (just uncomment) , and disconnect all other components that are connected to the fetcher : use marketFXSimulator instead as quote provider.
    case q: Quote =>
      //send(q)
      tradingPrices((q.whatC, q.withC)) = (q.bid, q.ask)

    case _ =>
      println("MS: got unknown")
  }
}
