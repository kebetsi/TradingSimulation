package ch.epfl.ts.traders

import scala.language.postfixOps
import scala.collection.mutable.{Map => MMap, MutableList => MList}
import scala.concurrent.duration.{DurationInt, DurationLong}
import akka.actor.Cancellable
import ch.epfl.ts.component.{Component, ComponentRef}
import ch.epfl.ts.data._
import ch.epfl.ts.data.Currency._

/**
  * Implements back testing for traders.
  *
  * To use this class, redirect all previous connections into trader to instances
  * of this class. Connections out of the original trader should remain unchanged.
  *
  * @param trader the reference to the real trader component
  * @param traderId the id of the trader
  * @param initial the initial seed money
  * @param currency currency of the intial seed money
  * @param period the period in milliseconds to compute performance
  */
class BacktestTrader(trader: ComponentRef, traderId: Long, initial: Double, currency: Currency, period: Long) extends Component {
  // for usage of scheduler
  import context._

  private var schedule: Cancellable = null
  private val wallet = MMap[Currency, Double](currency -> initial)
  private val returns = MList[Double]()

  private var lastValue = initial
  private var maxProfit = 0.0
  private var maxLoss = 0.0

  def totalReturns: Double = value() / initial * 100
  def volatility: Double = computeVolatility
  def drawdown: Double = maxLoss
  def sharp: Double = totalReturns / volatility

  // handle interested messages and forward all messages to the trader
  override def receiver = {
    case t: Transaction if t.buyerId == traderId =>  // buy
      trader.ar ! t
      buy(t)
    case t: Transaction if t.sellerId == traderId =>  // sell
      trader.ar ! t
      sell(t)
    case 'UpdateStatistics =>
      updateStatistic
    case m => trader.ar ! m
  }

  // returns the exchange ratio between two currency
  private def ratio(from: Currency, to: Currency): Double = ???

  // returns the total money of the wallet converted to the given currency
  private def value(in: Currency = currency): Double = {
    (wallet :\ 0.0) { case ((c, amount), acc) => acc + ratio(c, in) * amount }
  }

  // updates the wallet and statistics after a sell contraction
  private def sell(t: Transaction): Unit = {
    wallet += t.whatC -> (wallet.getOrElse(t.whatC, 0.0) - t.volume)
    wallet += t.withC -> (wallet.getOrElse(t.withC, 0.0) + t.volume * t.price)
  }

  // updates the wallet and statistics after a buy contraction
  private def buy(t: Transaction): Unit = {
    wallet += t.whatC -> (wallet.getOrElse(t.whatC, 0.0) + t.volume)
    wallet += t.withC -> (wallet.getOrElse(t.withC, 0.0) - t.volume * t.price)
  }

  // compute volatility, which is the variance of returns
  private def computeVolatility = {
    val mean = (returns :\ 0.0)(_ + _) / returns.length
    (returns :\ 0.0) { (r, acc) => (r - mean) * (r - mean) + acc } / returns.length
  }

  // updates the statistics
  private def updateStatistic = {
    val curVal = value()

    val profit = curVal - initial
    if (profit > maxProfit) maxProfit = profit
    else if (profit < maxLoss) maxLoss = profit

    returns += (curVal - lastValue) / lastValue

    lastValue = curVal
  }

  override def start = {
    schedule = context.system.scheduler.schedule(10.milliseconds, period.milliseconds, self, 'UpdateStatistics)
  }

  override def stop = {
    schedule.cancel()

    println(s"########### trader Id: $traderId ############")
    println(s"total returns: $totalReturns")
    println(s"volatility: $volatility")
    println(s"draw down: $drawdown")
    println(s"sharp: $sharp")
  }
}
