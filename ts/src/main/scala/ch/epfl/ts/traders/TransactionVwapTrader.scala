package ch.epfl.ts.traders

import scala.language.postfixOps
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationInt
import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{MarketAskOrder, MarketBidOrder, Transaction}
import ch.epfl.ts.data.{StrategyParameters, TimeParameter, NaturalNumberParameter, ParameterTrait}

/**
 * Transaction VWAP trader companion object
 */
object TransactionVwapTrader extends TraderCompanion {
  type ConcreteTrader = TransactionVwapTrader
  override protected val concreteTraderTag = scala.reflect.classTag[TransactionVwapTrader]
  
  /** Time frame */
  val TIME_FRAME = "TimeFrame"
  /** Volume to trade */
  val VOLUME = "Volume"
  
  // timeFrameMillis: Int
  override def requiredParameters = Map(
    TIME_FRAME -> TimeParameter,
    VOLUME -> NaturalNumberParameter
  )
}

/**
 * Transaction VWAP trader.
 */
class TransactionVwapTrader(uid: Long, parameters: StrategyParameters) extends Trader(parameters) {
  import context._
  override def companion = MadTrader
  
  case object Tick

  val timeFrame = parameters.get[FiniteDuration](TransactionVwapTrader.TIME_FRAME)
  val volumeToTrade = parameters.get[Int](TransactionVwapTrader.VOLUME)

  var transactions: List[Transaction] = Nil
  var cumulativeTPV: Double = 0.0;
  var cumulativeVolume: Double = 0.0;
  var vwap: Double = 0.0;
  var tradingPrice: Double = 0.0;

  var oid = uid

  def priceOrdering = new Ordering[Transaction] {
    def compare(first: Transaction, second: Transaction): Int =
      if (first.price > second.price) 1 else if (first.price < second.price) -1 else 0
  }

  def timeOrdering = new Ordering[Transaction] {
    def compare(first: Transaction, second: Transaction): Int =
      if (first.timestamp > second.timestamp) 1 else if (first.timestamp < second.timestamp) -1 else 0
  }
  
  def receiver = {
    case t: Transaction => transactions = t :: transactions
    case Tick => {
      computeVWAP
      if (tradingPrice > vwap) {
        // sell
        println("TransactionVWAPTrader: sending market ask order")
        send(MarketAskOrder(oid, uid, System.currentTimeMillis(), USD, USD, volumeToTrade, 0))
        oid = oid + 1
      } else {
        // buy
        println("TransactionVWAPTrader: sending market bid order")
        send(MarketBidOrder(oid, uid, System.currentTimeMillis(), USD, USD, volumeToTrade, 0))
        oid = oid + 1
      }
    }
    case _ => println("vwapTrader: unknown message received")
  }

  def computeVWAP() = {
    if (!transactions.isEmpty) {
      val typicalPrice = (transactions.max(priceOrdering)).price * (transactions.min(priceOrdering)).price * (transactions.max(timeOrdering)).price / 3
      var frameVolume: Double = 0
      transactions.map { t => frameVolume = frameVolume + t.volume }
      cumulativeVolume = cumulativeVolume + frameVolume
      val TPV = typicalPrice * frameVolume
      cumulativeTPV = cumulativeTPV + TPV
      vwap = cumulativeTPV / cumulativeVolume
      tradingPrice = transactions.max(timeOrdering).price
      transactions = Nil
    }
  }

  override def start = {
    println("TransactionVwapTrader: Started")
    system.scheduler.schedule(0 milliseconds, timeFrame, self, Tick)
  }
}