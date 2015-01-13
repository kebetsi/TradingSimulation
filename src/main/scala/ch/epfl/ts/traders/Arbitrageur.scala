package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.engine.OHLC
import ch.epfl.ts.component.fetch.MarketNames

class Arbitrageur(uid: Long) extends Component {

  // key: marketId, value: tradingPrice
  var marketPrices = Map[Long, Double]()
  // key: marketId, value: most recent OHLC
  var marketOhlcs = Map[Long, OHLC]()

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
    marketOhlcs.map(a => marketOhlcs.map(b => {
      if (a != b) {
        print("market " + MarketNames.marketIdToName(a._1) + ": price = " + a._2.close + ", market " + MarketNames.marketIdToName(b._1) + 
            ": price = " + b._2.close + ", difference = ")
        print(f"${a._2.close - b._2.close}%.2f, ")
        println(f"(${(a._2.close - b._2.close) / b._2.close}%.2f %%)")
        // calculate win if sold on 1 market, and bought on other, take in account commission
      }
    }))
  }
}