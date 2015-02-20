package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.component.fetch.MarketNames
import ch.epfl.ts.data.{ OHLC, Transaction, MarketAskOrder, MarketBidOrder }
import ch.epfl.ts.data.Currency._

/**
 * Arbitrageur trader: receives OHLCs from multiple markets
 */
class Arbitrageur(uid: Long) extends Component {

  var oid = 40000000L

  // key: marketId, value: tradingPrice
  var marketPrices = Map[Long, Double]()
  // key: marketId, value: most recent OHLC
  var marketOhlcs = Map[Long, OHLC]()
  // key: (marketIdA, marketIdB), value: (OhlcA.close - OhlcB.close)
  var marketCloseDifferences = Map[(Long, Long), Double]()

  def receiver = {
    case t: Transaction => {
      marketPrices += (t.mid -> t.price)
      //      println("Arbitrageur: MarketPrices")
      //      marketPrices.map(m => println("Market " + MarketNames.marketIdToName(m._1) + ": trading price = " + m._2))
    }
    case ohlc: OHLC => {
      marketOhlcs += (ohlc.marketId -> ohlc)
      println("Arbitrageur: received OHLC from " + MarketNames.marketIdToName(ohlc.marketId) + ": " + ohlc)
      computeOhlcDifferences
    }
    case _ => println("Arbitrageur: received unknown")
  }

  def computeOhlcDifferences = {
    val priceDiff = new StringBuffer()
    marketOhlcs.map(a => marketOhlcs.map(b => {
      if (a != b) {
        priceDiff.append("market " + MarketNames.marketIdToName(a._1) + ": price = " + a._2.close + ", market " + MarketNames.marketIdToName(b._1) +
          ": price = " + b._2.close + ", difference = ")
        val difference = a._2.close - b._2.close
        val percentageDifference = (a._2.close - b._2.close) / b._2.close
        marketCloseDifferences += (a._1, b._1) -> difference
        priceDiff.append(f"${difference}%.4f, ")
        priceDiff.append(f"(${percentageDifference}%.4f %%)\n")
        // calculate win if sold on 1 market, and bought on other, take commission into account
      }
    }))
    print(priceDiff)
    marketCloseDifferences.keySet.foreach { x =>
      {
        if (marketCloseDifferences(x) > 0) {
          // send buy to x._1, send sell to x._2
          MarketAskOrder(oid, uid, System.currentTimeMillis(), BTC, USD, 50.0, 0.0)
          oid = oid + 1
          MarketBidOrder(oid, uid, System.currentTimeMillis(), BTC, USD, 50.0, 0.0)
          oid = oid + 1

        }
      }
    }
  }
}