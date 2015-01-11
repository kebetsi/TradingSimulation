package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.indicators.{ MA, EMA, SMA }
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.MarketBidOrder

/**
 *
 */
class DoubleCrossoverTrader(val uid: Long, val shortPeriod: Int, val longPeriod: Long, val volume: Double) extends Component {

  var previousShortMa: Double = 0.0
  var previousLongMa: Double = 0.0
  var currentShortMa: Double = 0.0
  var currentLongMa: Double = 0.0
  var oid = 876543

  def receiver = {
    case ma: MA => ma.period match {
      case `shortPeriod` => {
        previousShortMa = currentShortMa
        currentShortMa = ma.value
        makeOrder
      }
      case `longPeriod` => {
        previousLongMa = currentLongMa
        currentLongMa = ma.value
        makeOrder
      }
    }
    case _ =>
  }

  def makeOrder = {
    if ((previousShortMa > previousLongMa) && (currentShortMa < currentLongMa)) {
      send(new MarketAskOrder(oid, uid, System.currentTimeMillis(), USD, USD, volume, 0))
      oid = oid + 1
    } else if ((previousShortMa < previousLongMa) && (currentShortMa > currentLongMa)) {
      send(new MarketBidOrder(oid, uid, System.currentTimeMillis(), USD, USD, volume, 0))
      oid = oid + 1
    }
  }
}