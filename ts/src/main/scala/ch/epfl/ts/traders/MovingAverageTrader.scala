package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.indicators.MovingAverage
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{ MarketAskOrder, MarketBidOrder, Quote }
import ch.epfl.ts.data.Currency
import akka.actor.ActorLogging
import ch.epfl.ts.engine.MarketFXSimulator
import akka.actor.ActorRef
import ch.epfl.ts.data.ConfirmRegistration
import ch.epfl.ts.data.Register
import ch.epfl.ts.engine.FundWallet
import ch.epfl.ts.engine.GetWalletFunds
import ch.epfl.ts.engine.AcceptedOrder
import ch.epfl.ts.engine.RejectedOrder
import ch.epfl.ts.engine.ExecutedAskOrder
import ch.epfl.ts.engine.ExecutedBidOrder
import scala.concurrent.duration.FiniteDuration
import ch.epfl.ts.data.CurrencyPairParameter
import ch.epfl.ts.data.NaturalNumberParameter
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.data.TimeParameter
import ch.epfl.ts.data.RealNumberParameter
import ch.epfl.ts.data.BooleanParameter

/**
 * MovingAverageTrader companion object
 */
object MovingAverageTrader extends TraderCompanion {
  type ConcreteTrader = MovingAverageTrader
  override protected val concreteTraderTag = scala.reflect.classTag[MovingAverageTrader]

  /** Currency pair to trade */
  val SYMBOL = "Symbol"
  /** Period for the shorter moving average **/
  val SHORT_PERIOD = "ShortPeriod"
  /** Period for the longer moving average **/
  val LONG_PERIOD = "LongPeriod"
  /** Volume to trade */
  val VOLUME = "Volume"
  /** Tolerance: a kind of sensitivity threshold to avoid "fake" buy signals */
  val TOLERANCE = "Tolerance"
  /** Allow the use of Short orders in the strategy */
  val WITH_SHORT = "WithShort"

  override def strategyRequiredParameters = Map(
    SYMBOL -> CurrencyPairParameter,
    SHORT_PERIOD -> TimeParameter,
    LONG_PERIOD -> TimeParameter,
    VOLUME -> RealNumberParameter,
    TOLERANCE -> RealNumberParameter
  )
  
  override def optionalParameters = Map(
    WITH_SHORT -> BooleanParameter
  )
}

/**
 * Simple momentum strategy.
 */
class MovingAverageTrader(uid: Long, parameters: StrategyParameters)
    extends Trader(uid, parameters) with ActorLogging {

  import context.dispatcher

  override def companion = MovingAverageTrader
  
  val symbol = parameters.get[(Currency, Currency)](MovingAverageTrader.SYMBOL)
  val shortPeriod = parameters.get[FiniteDuration](MovingAverageTrader.SHORT_PERIOD)
  val longPeriod = parameters.get[FiniteDuration](MovingAverageTrader.LONG_PERIOD)
  val volume = parameters.get[Double](MovingAverageTrader.VOLUME)
  val tolerance = parameters.get[Double](MovingAverageTrader.TOLERANCE)
  val withShort = parameters.getOrElse[Boolean](MovingAverageTrader.WITH_SHORT, false)

  /**
   * Broker information
   */
  var broker: ActorRef = null
  var registered = false

  /**
   * Moving average of the current period
   */
  var currentShort: Double = 0.0
  var currentLong: Double = 0.0

  var oid = 0

  // TODO: replace by being wallet-aware
  var holdings: Double = 0.0
  var shortings: Double = 0.0

  val (whatC, withC) = symbol

  override def receiver = {

    case ConfirmRegistration => {
      broker = sender()
      registered = true
      log.debug("TraderWithB: Broker confirmed")
    }

    case ma: MovingAverage => {
      println("Trader receive MAs")
      ma.value.get(shortPeriod.length.toInt) match {
        case Some(x) => currentShort = x
        case None    => println("Error: Missing indicator with period " + shortPeriod)
      }
      ma.value.get(longPeriod.length.toInt) match {
        case Some(x) => currentLong = x
        case None    => println("Error: Missing indicator with period " + longPeriod)
      }

      decideOrder
    }

    // Transaction has been accepted by the broker (but may not be executed : e.g. limit orders) = OPEN Positions
    case _: AcceptedOrder => // TODO SimplePrint / Log /.../Frontend log ??

    // Order has been executed on the market = CLOSE Positions
    case _: ExecutedBidOrder =>// TODO SimplePrint / Log /.../Frontend log ??
    case _: ExecutedAskOrder => // TODO SimplePrint/Log/.../Frontend log ??

    // If we receive a Rejected Order, we stop the trader
    case _: RejectedOrder=> stop

    case whatever => println("SimpleTrader: received unknown : " + whatever)
  }

  def decideOrder =
    if (withShort) decideOrderWithShort
    else decideOrderWithoutShort

  def decideOrderWithoutShort = {
    // BUY signal
    if (currentShort > currentLong * (1 + tolerance) && holdings == 0.0) {
      log.debug("buying " + volume)
      send(MarketBidOrder(oid, uid, System.currentTimeMillis(), whatC, withC, volume, -1))
      oid += 1
      holdings = volume
    }
    // SELL signal
    else if (currentShort < currentLong && holdings > 0.0) {
      log.debug("selling " + volume)
      send(MarketAskOrder(oid, uid, System.currentTimeMillis(), whatC, withC, volume, -1))
      oid += 1
      holdings = 0.0
    }
  }

  def decideOrderWithShort = {
    // BUY signal
    if (currentShort > currentLong) {
      if (shortings > 0.0) {
        log.debug("closing short " + volume)
        send(MarketBidOrder(oid, uid, System.currentTimeMillis(), whatC, withC, volume, -1))
        oid += 1;
        shortings = 0.0;
      }
      if (currentShort > currentLong * (1 + tolerance) && holdings == 0.0) {
        log.debug("buying " + volume)
        send(MarketBidOrder(oid, uid, System.currentTimeMillis(), whatC, withC, volume, -1))
        oid += 1
        holdings = volume
      }
    }
    // SELL signal
    else if (currentShort < currentLong) {
      if (holdings > 0.0) {
        log.debug("selling " + volume)
        send(MarketAskOrder(oid, uid, System.currentTimeMillis(), whatC, withC, volume, -1))
        oid += 1
        holdings = 0.0
      }
      if (currentShort * (1 + tolerance) < currentLong && shortings == 0.0) {
        log.debug("short " + volume)
        send(MarketAskOrder(oid, uid, System.currentTimeMillis(), whatC, withC, volume, -1))
        oid += 1;
        shortings = volume;
      }
    }
  }

  override def init = {
    log.debug("MovingAverageTrader received startSignal")
  }

}
