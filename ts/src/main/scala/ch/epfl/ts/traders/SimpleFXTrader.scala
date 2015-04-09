package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.indicators.MA
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{ MarketAskOrder, MarketBidOrder, Quote }
import ch.epfl.ts.data.Currency

/* This simple trader will use two moving average and send order when this two MA cross each other.
 * @ Param the length of the two moving average period.
 */

//symbol format  EUR/CHF , CHF/USD .. , as string => easier for user. 
class SimpleFXTrader(val uid: Long, symbol: (Currency, Currency), val shortPeriod: Int, val longPeriod: Int, val volume: Double) extends Component {

  //contains the moving average obtained one period before ( used to detect the point when the two MA cross )
  var previousShort: Double = 0.0
  var previousLong: Double = 0.0
  var init = true
  // stock the current Moving average
  var currentShort: Double = 0.0
  var currentLong: Double = 0.0

  //TODO what is a good initialization oid
  var oid = 12345

  val (whatC, withC) = symbol

  override def receiver = {

    case ma: MA => {
      println("Trader receive MAs")
      ma.value.get(shortPeriod) match {
        case Some(x) => currentShort = x
        case None    => println("Missing indicator with period " + shortPeriod)
      }
      ma.value.get(longPeriod) match {
        case Some(x) => currentLong = x
        case None    => println("Missing indicator with period " + longPeriod)
      }
      //Check signals and make appropriate orders
      if (init) {
        previousShort = currentShort
        previousLong = currentLong
        init = false
      }
      decideOrder()
    }

    case _ => println("SimpleTrader: received unknown")
  }
  def decideOrder() = {
    if (previousShort < currentLong && currentShort > currentLong) {
      //price specified only for limit orders
      send(MarketBidOrder(oid, uid, System.currentTimeMillis(), Currency.EUR, Currency.USD, volume, -1))
      println("simple trader : buying")
      oid += 1
    } else if (previousShort > currentLong && currentShort < currentLong) {
      //price specified only for limit orders
      send(MarketAskOrder(oid, uid, System.currentTimeMillis(), Currency.EUR, Currency.USD, volume, -1))
      println("simple trader : selling")
      oid += 1
    }
    previousShort = currentShort
    previousLong = currentLong
  }
}