package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{MarketAskOrder, MarketBidOrder, Transaction}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/**
 * Transaction VWAP trader.
 */
class TransactionVwapTrader(uid: Long, timeFrameMillis: Int) extends Component {
  import context._

  case object Tick

  def priceOrdering = new Ordering[Transaction] {
    def compare(first: Transaction, second: Transaction): Int =
      if (first.price > second.price) 1 else if (first.price < second.price) -1 else 0
  }

  def timeOrdering = new Ordering[Transaction] {
    def compare(first: Transaction, second: Transaction): Int =
      if (first.timestamp > second.timestamp) 1 else if (first.timestamp < second.timestamp) -1 else 0
  }

  var transactions: List[Transaction] = Nil
  var cumulativeTPV: Double = 0.0;
  var cumulativeVolume: Double = 0.0;
  var vwap: Double = 0.0;
  var tradingPrice: Double = 0.0;

  var oid = uid
  val volumeToTrade = 50

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
    system.scheduler.schedule(0 milliseconds, timeFrameMillis milliseconds, self, Tick)
  }
}