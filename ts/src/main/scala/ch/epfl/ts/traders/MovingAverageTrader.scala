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

/**
 * Simple momentum strategy.
 * @param symbol the pair of currency we are trading with
 * @param shortPeriod the size of the rolling window of the short moving average
 * @param longPeriod the size of the rolling window of the long moving average
 * @param volume the amount that we want to buy when buy signal
 * @param tolerance is required to avoid fake buy signal
 * @param withShort version with/without short orders
 */
class MovingAverageTrader(val uid: Long, symbol: (Currency, Currency),
                          val shortPeriod: Int, val longPeriod: Int,
                          val volume: Double, val tolerance: Double, val withShort: Boolean) extends Component with ActorLogging {

  import context.dispatcher
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
      ma.value.get(shortPeriod) match {
        case Some(x) => currentShort = x
        case None    => println("Error: Missing indicator with period " + shortPeriod)
      }
      ma.value.get(longPeriod) match {
        case Some(x) => currentLong = x
        case None    => println("Error: Missing indicator with period " + longPeriod)
      }

      decideOrder
    }

    case whatever => println("SimpleTrader: received unknown : " + whatever)
  }

  def decideOrder =
    if (withShort) decideOrderWithShort
    else decideOrderWithoutShort

  def decideOrderWithoutShort = {
    //BUY signal
    if (currentShort > currentLong * (1 + tolerance) && holdings == 0.0) {
      log.debug("buying " + volume)
      send(MarketBidOrder(oid, uid, System.currentTimeMillis(), whatC, withC, volume, -1))
      oid += 1
      holdings = volume
    } //SELL signal
    else if (currentShort < currentLong && holdings > 0.0) {
      log.debug("selling " + volume)
      send(MarketAskOrder(oid, uid, System.currentTimeMillis(), whatC, withC, volume, -1))
      oid += 1
      holdings = 0.0
    }
  }

  def decideOrderWithShort = {
    //BUY signal
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
    } //SELL signal
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

  override def start = {
    log.debug("MovingAverageTrader received startSignal")
    send(Register(uid))
    send(FundWallet(uid, Currency.CHF, 5000))
    send(GetWalletFunds(uid))
  }

}