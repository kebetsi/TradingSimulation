package ch.epfl.ts.evaluation

import scala.language.postfixOps
import scala.collection.mutable.{Map => MMap, MutableList => MList}
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.concurrent.duration.FiniteDuration
import akka.actor.{ActorRef, Cancellable}
import ch.epfl.ts.component.{ComponentRegistration, Component, ComponentRef}
import ch.epfl.ts.data._
import ch.epfl.ts.data.Currency._

/**
 * Represents metrics of a strategy
 */
case class EvaluationReport(traderName: String, wallet: Map[Currency, Double], currency: Currency, initial: Double,
                            current: Double, totalReturns: Double, volatility: Double, drawdown: Double, sharpeRatio: Double)

/**
  * Evaluates the performance of traders by total returns, volatility, draw down and sharpe ratio
  *
  * To use this class, redirect all previous connections into and out of the trader to
  * instances of this class.
  *
  * Note that a component providing Quote data must be connected to this component in order for
  * it to maintain a price table and calculate profit/loss correctly.
  *
  * @param trader the reference to the trader component
  * @param traderId the id of the trader
  * @param initial the initial seed money
  * @param currency currency of the initial seed money
  * @param period the time period to send evaluation report
  */
class Evaluator(trader: ComponentRef, traderId: Long, initial: Double, currency: Currency, period: FiniteDuration) extends Component {
  // for usage of scheduler
  import context._

  private var schedule: Cancellable = null
  private val wallet = MMap[Currency, Double](currency -> initial)
  private val returnsList = MList[Double]()
  private val priceTable = MMap[(Currency, Currency), Double]()

  private var lastValue = initial
  private var maxProfit = 0.0

  /** Max loss as a positive value
   */
  private var maxLoss = 0.0

  /**
   * Redirects out-going connections to the trader
   */
  override def connect(ar: ActorRef, ct: Class[_], name: String) = {
    if (ct.equals(classOf[EvaluationReport]))
      dest += (ct -> (ar :: dest.getOrElse(ct, List())))
    else
      trader.ar ! ComponentRegistration(ar, ct, name)
  }

  /**
   * Handles interested messages and forward all messages to the trader
   */
  override def receiver = {
    case t: Transaction if t.buyerId == traderId =>  // buy
      trader.ar ! t
      buy(t)
    case t: Transaction if t.sellerId == traderId =>  // sell
      trader.ar ! t
      sell(t)
    case q: Quote =>
      updatePrice(q)
    case 'Report =>
      if (canReport) report
    case m => trader.ar ! m
  }

  /**
   *  Returns the exchange ratio between two currency
   *
   *  1 unit of currency *from* can buy *ratio* units of currency *to*
   */
  private def ratio(from: Currency, to: Currency): Double = {
    if (from == to) 1.0 else priceTable(from -> to)
  }

  /**
   *  Returns the total money of the wallet converted to the given currency
   */
  private def value(in: Currency = currency): Double = {
    (wallet :\ 0.0) { case ((c, amount), acc) => acc + ratio(c, in) * amount }
  }

  /**
   *  Updates the wallet and statistics after a sell transaction
   */
  private def sell(t: Transaction): Unit = {
    wallet += t.whatC -> (wallet.getOrElse(t.whatC, 0.0) - t.volume)
    wallet += t.withC -> (wallet.getOrElse(t.withC, 0.0) + t.volume * t.price)
  }

  /**
   * Updates the wallet and statistics after a buy transaction
   */
  private def buy(t: Transaction): Unit = {
    wallet += t.whatC -> (wallet.getOrElse(t.whatC, 0.0) + t.volume)
    wallet += t.withC -> (wallet.getOrElse(t.withC, 0.0) - t.volume * t.price)
  }

  /**
   * Updates the price table
   */
  private def updatePrice(q: Quote) = {
    val Quote(_, _, whatC, withC, bid, ask) = q
    priceTable.put(whatC -> withC, (bid + ask) / 2.0)
    priceTable.put(withC -> whatC, (1.0/bid + 1.0/ask) / 2.0)
  }

  /**
   * Computes volatility, which is the variance of returns
   */
  private def computeVolatility = {
    val mean = (returnsList :\ 0.0)(_ + _) / returnsList.length
    (returnsList :\ 0.0) { (r, acc) => (r - mean) * (r - mean) + acc } / returnsList.length
  }

  /**
   * Updates the statistics
   */
  private def report: Unit = {
    val curVal = value()

    val profit = curVal - initial
    if (profit > maxProfit) maxProfit = profit
    else if (profit < 0 && 0 - profit > maxLoss) maxLoss = 0 - profit

    returnsList += (curVal - lastValue) / lastValue

    lastValue = curVal

    // Generate report
    //TODO find appropriate value for risk free rate
    val riskFreeRate = 0.03
    val totalReturns = (value() - initial) / initial
    val volatility = computeVolatility
    val drawdown = maxLoss / initial
    val sharpeRatio = (totalReturns - riskFreeRate) / volatility

    send(EvaluationReport(trader.name, wallet.toMap, currency, initial, curVal, totalReturns, volatility, drawdown, sharpeRatio))
  }

  /**
   * Tells whether it's OK to report
   * @return True if and only if each currency appearing in the wallet also appears in the price table.
   * */
  private def canReport: Boolean = {
    if (priceTable.size == 0) false
    else wallet.keys.forall(c => c == currency || priceTable.contains(c -> currency))
  }

  /**
   * Starts the scheduler and trader
   */
  override def start = {
    schedule = context.system.scheduler.schedule(2000.milliseconds, period, self, 'Report)
  }

  /**
   * Stops the scheduler and trader
   */
  override def stop = {
    schedule.cancel()

    // Last report
    if (canReport) report
  }
}
