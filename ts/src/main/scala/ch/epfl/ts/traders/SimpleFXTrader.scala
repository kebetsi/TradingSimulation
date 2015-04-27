package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.indicators.MovingAverage
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{ MarketAskOrder, MarketBidOrder, Quote }
import ch.epfl.ts.data.Currency

/* This simple trader will use two moving average and send order when this two MA cross each other.
 * @ Param the length of the two moving average period.
 */

//symbol format  EUR/CHF , CHF/USD .. , as string => easier for user. 
class SimpleFXTrader(val uid: Long, symbol: (Currency, Currency),
                    val shortPeriod: Int, val longPeriod: Int,
                    val volume: Double) extends Component {

  /**
   * Boolean flag which indicates when enough indications have been
   * received to start making decisions (like a buffering mechanism)
   */
  var hasStarted = false
  /**
   * Moving average of the previous period (used to detect the point when the two MA cross)
   */
  var previousShort: Double = 0.0
  var previousLong: Double = 0.0
  /**
   * Moving average of the current period
   */
  var currentShort: Double = 0.0
  var currentLong: Double = 0.0

  // TODO: Need to ensure unique order ids
  var oid = 12345

  val (whatC, withC) = symbol

  override def receiver = {

    case ma: MovingAverage => {
      println("Trader receive MAs")
      ma.value.get(shortPeriod) match {
        case Some(x) => currentShort = x
        case None    => println("Missing indicator with period " + shortPeriod)
      }
      ma.value.get(longPeriod) match {
        case Some(x) => currentLong = x
        case None    => println("Missing indicator with period " + longPeriod)
      }
      
      // We can't take decisions before knowing at least one `previousShort`, `previousLong`
      if (hasStarted){
        decideOrder()
      }
      else{
       previousShort = currentShort
       previousLong = currentLong
       hasStarted = true
      }

    }

    case _ => println("SimpleTrader: received unknown")
  }
  
  def decideOrder() = {
    // The two MA cross and short moving average is above long average: BUY signal
    if (previousShort < currentLong && currentShort > currentLong) {
      // Price needs to be specified only for limit orders
      send(MarketBidOrder(oid, uid, System.currentTimeMillis(), whatC, withC, volume, -1))
      println("simple trader : buying")
      oid += 1
    }
    // The two MA cross and short moving average is below long average: SELL signal
    else if (previousShort > currentLong && currentShort < currentLong) {
      // Price needs to be specified only for limit orders
      send(MarketAskOrder(oid, uid, System.currentTimeMillis(), whatC, withC, volume, -1))
      println("simple trader : selling")
      oid += 1
    }
    previousShort = currentShort
    previousLong = currentLong
  }
}