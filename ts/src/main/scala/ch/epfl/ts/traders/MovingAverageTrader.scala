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
import scala.slick.direct.order
import scala.language.postfixOps
import akka.pattern.ask
import ch.epfl.ts.data._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.collection.mutable.{ HashMap => MHashMap }
import ch.epfl.ts.engine.WalletFunds
import scala.math.abs
import scala.math.floor
import ch.epfl.ts.engine.Wallet

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
  /** Tolerance: a kind of sensitivity threshold to avoid "fake" buy signals */
  val TOLERANCE = "Tolerance"
  /** Allow the use of Short orders in the strategy */
  val WITH_SHORT = "WithShort"

  override def strategyRequiredParameters = Map(
    SYMBOL -> CurrencyPairParameter,
    SHORT_PERIOD -> TimeParameter,
    LONG_PERIOD -> TimeParameter,
    TOLERANCE -> RealNumberParameter)

  override def optionalParameters = Map(
    WITH_SHORT -> BooleanParameter)
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

  var tradingPrices = MHashMap[(Currency, Currency), (Double, Double)]()

  override def receiver = {

    case q: Quote => {
      tradingPrices((q.whatC, q.withC)) = (q.bid, q.ask)
    }

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

    // Order has been executed on the market = CLOSE Positions
    case _: ExecutedBidOrder => // TODO SimplePrint / Log /.../Frontend log ??
    case _: ExecutedAskOrder => // TODO SimplePrint/Log/.../Frontend log ??

    case whatever            => println("SimpleTrader: received unknown : " + whatever)
  }
  def decideOrder = {
    var volume = 0.0
    var holdings = 0.0
    var shortings = 0.0

    implicit val timeout = new Timeout(500 milliseconds)
    val future = (broker ? GetWalletFunds(uid)).mapTo[WalletFunds]
    future onSuccess {
      case WalletFunds(id, funds: Map[Currency, Double]) => {
        val cashWith = funds.getOrElse(withC, 0.0)
        log.debug("cashWith" + cashWith)
        holdings = funds.getOrElse(whatC, 0.0)
        if (holdings < 0.0) {
          shortings = abs(holdings)
          holdings = 0.0
        }
        val askPrice = tradingPrices(whatC, withC)._2
        //Prevent slippage leading to insufisent funds
        volume = floor(cashWith / askPrice)
        if (withShort) {
          decideOrderWithShort(volume, holdings, shortings)
        } else {
          decideOrderWithoutShort(volume, holdings)
        }
      }
    }
    future onFailure {
      case p => {
        log.debug("MA Trader : Wallet command failed : " + p)
        stop
      }
    }

  }

  def decideOrderWithoutShort(volume: Double, holdings: Double) = {
    // BUY signal
    if (currentShort > currentLong * (1 + tolerance) && holdings == 0.0) {
      log.debug("buying " + volume)
      placeOrder(MarketBidOrder(oid, uid, System.currentTimeMillis(), whatC, withC, volume, -1))
      oid += 1
    } // SELL signal
    else if (currentShort < currentLong && holdings > 0.0) {
      log.debug("selling " + holdings)
      placeOrder(MarketAskOrder(oid, uid, System.currentTimeMillis(), whatC, withC, holdings, -1))
      oid += 1
    }
  }

  def decideOrderWithShort(volume: Double, holdings: Double, shortings: Double) = {
    // BUY signal
    if (currentShort > currentLong) {
      if (shortings > 0.0) {
        log.debug("closing short " + shortings)
        placeOrder(MarketBidOrder(oid, uid, System.currentTimeMillis(), whatC, withC, shortings, -1))
        oid += 1;
      }
      if (currentShort > currentLong * (1 + tolerance) && holdings == 0.0) {
        log.debug("buying " + volume)
        placeOrder(MarketBidOrder(oid, uid, System.currentTimeMillis(), whatC, withC, volume, -1))
        oid += 1
      }
    } // SELL signal
    else if (currentShort < currentLong) {
      if (holdings > 0.0) {
        log.debug("selling " + holdings)
        placeOrder(MarketAskOrder(oid, uid, System.currentTimeMillis(), whatC, withC, holdings, -1))
        oid += 1
      }
      if (currentShort * (1 + tolerance) < currentLong && shortings == 0.0) {
        log.debug("short " + volume)
        placeOrder(MarketAskOrder(oid, uid, System.currentTimeMillis(), whatC, withC, volume, -1))
        oid += 1;
      }
    }
  }

  def placeOrder(order: MarketOrder) = {
    implicit val timeout = new Timeout(500 milliseconds)
    val future = (broker ? order).mapTo[Order]
    future onSuccess {
      //Transaction has been accepted by the broker (but may not be executed : e.g. limit orders) = OPEN Positions
      case _: AcceptedOrder => log.debug("MATrader: order placement succeeded")
      case _: RejectedOrder => {
        log.debug("MATrader: order failed")
      }
      case _ => {
        log.debug("MATrader: unknown order response")
      }
    }
    future onFailure {
      case p => {
        log.debug("Wallet command failed: " + p)
      }
    }
  }

  override def init = {
    log.debug("MovingAverageTrader received startSignal")
  }

}
