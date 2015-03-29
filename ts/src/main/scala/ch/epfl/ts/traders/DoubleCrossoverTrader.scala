package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{MarketAskOrder, MarketBidOrder}
import ch.epfl.ts.indicators.MA

/**
 * Double Crossover Trader: receives moving average values of 2 different sizes (one must be a multiple of the other).
 * When the short MA crosses the long MA from above, a sell order is sent with the provided volume
 * When the short MA crosses the long MA from below, a buy order is sent with the provided volume
 */
class DoubleCrossoverTrader(val uid: Long, val shortPeriod: Int, val longPeriod: Int, val volume: Double) extends Component {

  var previousShortMa: Double = 0.0
  var previousLongMa: Double = 0.0
  var currentShortMa: Double = 0.0
  var currentLongMa: Double = 0.0
  val maSizeDiff: Int = longPeriod / shortPeriod
  var shortMaCount: Int = 0
  var isShortReady: Boolean = false
  var isLongReady: Boolean = false
  var oid = 876543

  def receiver = {
    case ma: MA =>
      println("DoubleCrossoverTrader: received " + ma); ma.period match {
        case `shortPeriod` => {
          shortMaCount = shortMaCount + 1
          if (shortMaCount == maSizeDiff) {
            isShortReady = true
            previousShortMa = currentShortMa
            currentShortMa = ma.value
            makeOrder
          }
        }
        case `longPeriod` => {
          isLongReady = true
          previousLongMa = currentLongMa
          currentLongMa = ma.value
          makeOrder
        }
      }
    case _ =>
  }

  def makeOrder = {
    if (isShortReady && isLongReady) {
      println("DoubleCrossoverTrader: both MAs received, checking possible orders:\n" +
        "oldShortMa=" + previousShortMa + ", oldLongMa=" + previousLongMa + "\n" + 
        "currentShortMa=" + currentShortMa + ", currentLongMa=" + currentLongMa)
      if ((previousShortMa > previousLongMa) && (currentShortMa < currentLongMa)) {
        send(MarketAskOrder(oid, uid, System.currentTimeMillis(), USD, USD, volume, 0))
        println("DoubleCrossoverTrader: sending sell")
        oid = oid + 1
      } else if ((previousShortMa < previousLongMa) && (currentShortMa > currentLongMa)) {
        send(MarketBidOrder(oid, uid, System.currentTimeMillis(), USD, USD, volume, 0))
        println("DoubleCrossoverTrader: sending buy")
        oid = oid + 1
      }
      isShortReady = false
      isLongReady = false
      shortMaCount = 0
    }
  }
}