package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.indicators.MovingAverage
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{ MarketAskOrder, MarketBidOrder, Quote }
import ch.epfl.ts.data.Currency

/**
 * Simple momentum strategy. 
 * @param symbol the pair of currency we are trading with
 * @param shortPeriod the size of the rolling window of the short moving average
 * @param longPeriod the size of the rolling window of the long moving average 
 * @param volume the amount that we want to buy when buy signal
 * @param tolerance is required to avoid fake buy signal  
 */
class SimpleFXTrader(val uid: Long, symbol: (Currency, Currency),
                    val shortPeriod: Int, val longPeriod: Int,
                    val volume: Double, val tolerance : Double) extends Component {



  /**
   * Moving average of the current period
   */
  var currentShort: Double = 0.0
  var currentLong: Double = 0.0

  // TODO: Need to ensure unique order ids
  var oid = 12345
  
  /**
   * 
   */
  var holdings: Double = 0.0

  val (whatC, withC) = symbol

  override def receiver = {

    case ma: MovingAverage => {
      println("Trader receive MAs")
      ma.value.get(shortPeriod) match {
        case Some(x) => currentShort = x
        case None    => println("Error: Missing indicator with period " + shortPeriod)
      }
      ma.value.get(longPeriod) match {
        case Some(x) => currentLong = x
        case None    => println("Error: Missing indicator with period " + longPeriod)
      }
      decideOrder()
    }

    case _ => println("SimpleTrader: received unknown")
  }
  
  def decideOrder() = {

    //BUY signal
    if (currentShort > currentLong * (1 + tolerance) && holdings == 0.0 ) {
      println("buy signal")
      send(MarketBidOrder(oid, uid, System.currentTimeMillis(), whatC, withC, volume, -1))
      oid += 1
      holdings = volume 
    }
    
    //SELL signal
    else if (currentShort < currentLong && holdings > 0.0) {
      println("sell signal")
      send(MarketAskOrder(oid, uid, System.currentTimeMillis(), whatC, withC, volume, -1))
      oid += 1
      holdings = 0.0
    }
  }
}