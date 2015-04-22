package ch.epfl.ts.traders

import scala.concurrent.duration.FiniteDuration

import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.Currency
import ch.epfl.ts.data.CurrencyPairParameter
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.data.NaturalNumberParameter
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.data.TimeParameter
import ch.epfl.ts.indicators.MovingAverage

/**
 * SimpleFXTrader companion object
 */
object SimpleFXTrader extends TraderCompanion {
  type ConcreteTrader = SimpleFXTrader
  override protected val concreteTraderTag = scala.reflect.classTag[SimpleFXTrader]
  
  /** Currency pair to trade */
  val SYMBOL = "Symbol"
  /** Period for the shorter moving average **/
  val SHORT_PERIOD = "ShortPeriod"
  /** Period for the longer moving average **/
  val LONG_PERIOD = "LongPeriod"
  /** Volume to trade */
  val VOLUME = "Volume"
  
  override def requiredParameters = Map(
    SYMBOL -> CurrencyPairParameter,
    SHORT_PERIOD -> TimeParameter,
    LONG_PERIOD -> TimeParameter,
    VOLUME -> NaturalNumberParameter
  )
}

/**
 * This simple trader will use two moving average and send order when this two MA cross each other.
 * @param shortPeriod Length of the short Moving Average period
 * @param longPeriod Length of the long Moving Average period
 */
class SimpleFXTrader(val uid: Long, parameters: StrategyParameters) extends Trader(parameters) {
  override def companion = SimpleFXTrader
  
  val symbol = parameters.get[(Currency, Currency)](SimpleFXTrader.SYMBOL)
  val shortPeriod = parameters.get[FiniteDuration](SimpleFXTrader.SHORT_PERIOD)
  val longPeriod = parameters.get[FiniteDuration](SimpleFXTrader.LONG_PERIOD)
  val volume = parameters.get[Int](SimpleFXTrader.VOLUME)
  
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
      ma.value.get(shortPeriod.length.toInt) match {
        case Some(x) => currentShort = x
        case None    => println("Missing indicator with period " + shortPeriod)
      }
      ma.value.get(longPeriod.length.toInt) match {
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
      send(MarketBidOrder(oid, uid, System.currentTimeMillis(), Currency.EUR, Currency.USD, volume, -1))
      println("simple trader : buying")
      oid += 1
    }
    // The two MA cross and short moving average is below long average: SELL signal
    else if (previousShort > currentLong && currentShort < currentLong) {
      // Price needs to be specified only for limit orders
      send(MarketAskOrder(oid, uid, System.currentTimeMillis(), Currency.EUR, Currency.USD, volume, -1))
      println("simple trader : selling")
      oid += 1
    }
    previousShort = currentShort
    previousLong = currentLong
  }
}
