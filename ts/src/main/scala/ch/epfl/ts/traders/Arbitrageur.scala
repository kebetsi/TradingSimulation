package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.component.fetch.MarketNames.marketIdToName
import ch.epfl.ts.data.{ OHLC, Transaction, MarketAskOrder, MarketBidOrder }
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.data.NaturalNumberParameter
import ch.epfl.ts.data.RealNumberParameter

/**
 * Arbitrageur companion object
 */
object Arbitrageur extends TraderCompanion {
  type ConcreteTrader = Arbitrageur
  override protected val concreteTraderTag = scala.reflect.classTag[Arbitrageur]
  
  /**
   * Minimum gap in price to trigger a trade
   */
  val PRICE_DELTA = "PriceDelta"
  /**
   * Volume to be traded
   */
  val VOLUME = "Volume"
  
  override def strategyRequiredParameters = Map(
    PRICE_DELTA -> RealNumberParameter,
    VOLUME -> NaturalNumberParameter
  )
}

/**
 * Arbitrageur trader: receives Transactions from multiple markets and sends market orders
 * to the exchanges when a certain delta price difference is reached.
 */
class Arbitrageur(uid: Long, parameters: StrategyParameters) extends Trader(uid, parameters) {
  override def companion = Arbitrageur
  
  var oid = 40000000L

  val priceDelta = parameters.get[Double](Arbitrageur.PRICE_DELTA)
  val volume = parameters.get[Int](Arbitrageur.VOLUME)
  
  // key: marketId, value: tradingPrice
  var marketPrices = Map[Long, Double]()
  // key: (marketIdA, marketIdB), value: (tradingPriceA - tradingPriceB)
  var marketPriceDifferences = Map[(Long, Long), Double]()

  def receiver = {
    case t: Transaction => {
      marketPrices += (t.mid -> t.price)
      computePriceDifferences(t.mid, t.price)
    }
    case _ => println("Arbitrageur: received unknown")
  }

  def computePriceDifferences(mId: Long, price: Double) = {

    // message base (mainly for debugging)
    val priceDifferencesDisplay = new StringBuffer("Arbitrageur:\n")

    marketPrices.map(a =>
      if (a._1 != mId) {
        // compute price difference
        val difference = price - a._2
        marketPriceDifferences += (mId, a._1) -> difference

        // send orders if price delta is reached
        if (difference > priceDelta) {
          // trading price of market with id=mId > trading price of market with id=a._1
          // sell mId shares, buy a._1 shares
          println("Arbitrageur: sending sell to " + marketIdToName(mId) + " and buy to " + marketIdToName(a._1))
          send(marketIdToName(mId), MarketAskOrder(oid, uid, System.currentTimeMillis(), BTC, USD, volume, 0.0))
          oid = oid + 1
          send(marketIdToName(a._1), MarketBidOrder(oid, uid, System.currentTimeMillis(), BTC, USD, volume, 0.0))
          oid = oid + 1
        } else if (-difference > priceDelta) {
          // trading price of market with id=a._1 > trading price of market with id=mId
          // sell a._1 shares, buy mId shares
          println("Arbitrageur: sending sell to " + marketIdToName(a._1) + " and buy to " + marketIdToName(mId))
          send(marketIdToName(a._1), MarketAskOrder(oid, uid, System.currentTimeMillis(), BTC, USD, volume, 0.0))
          oid = oid + 1
          send(marketIdToName(mId), MarketBidOrder(oid, uid, System.currentTimeMillis(), BTC, USD, volume, 0.0))
          oid = oid + 1
        }

        // compute price difference percentage (only for display)
        val percentageDifference = (price - a._2) / a._2
        priceDifferencesDisplay.append("market " + marketIdToName(mId) + ": price = " + price + ", market " + marketIdToName(a._1) +
          ": price = " + a._2 + ", difference = ")
        priceDifferencesDisplay.append(f"${difference}%.4f, ")
        priceDifferencesDisplay.append(f"(${percentageDifference}%.4f %%)\n")
      })
    print(priceDifferencesDisplay)

  }
}