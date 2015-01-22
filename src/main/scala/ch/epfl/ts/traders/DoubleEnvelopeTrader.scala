package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{MarketAskOrder, MarketBidOrder, OHLC}
import ch.epfl.ts.indicators.MA

class DoubleEnvelopeTrader(uid: Long, alpha: Double, volume: Double) extends Component {

  var oid = 23467
  var envBelow: Double = 0.0
  var envAbove: Double = 0.0
  var currentPrice: Double = 0.0

  def receiver = {
    case ma: MA => {
      envBelow = ma.value * (1 - alpha)
      envAbove = ma.value * (1 + alpha)
      if (currentPrice > envAbove) {
        // sell
        send(MarketAskOrder(oid, uid, System.currentTimeMillis(), USD, USD, volume, 0))
        oid = oid + 1
      }
      if (currentPrice < envBelow) {
        // buy
        send(MarketBidOrder(oid, uid, System.currentTimeMillis(), USD, USD, volume, 0))
        oid = oid + 1
      }
    }
    case o: OHLC => currentPrice = o.close
    case _       =>
  }
}