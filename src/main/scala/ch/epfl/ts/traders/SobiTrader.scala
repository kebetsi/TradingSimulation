package ch.epfl.ts.traders

import akka.actor.ActorRef
import ch.epfl.ts.component.Component
import scala.concurrent.duration.DurationInt
import ch.epfl.ts.engine.RetrieveBooks
import ch.epfl.ts.engine.Books
import scala.collection.mutable.PriorityQueue
import ch.epfl.ts.data.{Order, LimitAskOrder, LimitBidOrder}
import ch.epfl.ts.data.Currency._
import scala.collection.mutable.TreeSet
import ch.epfl.ts.component.StartSignal

case class FetchBooks()

class SobiTrader(intervalMillis: Int, quartile: Int, theta: Double, orderVolume: Int, priceDelta: Double)
  extends Component {
  import context._

  var bi: Double = 0.0
  var si: Double = 0.0
  val myId = 123
  var baseOrderId: Long = 456789

  override def receiver = {
    case StartSignal() => start

    case FetchBooks =>
      send(RetrieveBooks)

    case b: Books => {
      bi = computeBiOrSi(b.bids)
      si = computeBiOrSi(b.asks)
      if ((si - bi) > theta) {
        baseOrderId = baseOrderId + 1
        //"place an order to buy x shares at (lastPrice-p)"
        println("SobiTrader: making buy order: price=" + (b.tradingPrice - priceDelta) + ", volume=" + orderVolume)
        send(new LimitBidOrder(myId, baseOrderId, System.currentTimeMillis, USD, USD, orderVolume, b.tradingPrice - priceDelta))
      }
      if ((bi - si) > theta) {
        baseOrderId = baseOrderId + 1
        //"place an order to sell x shares at (lastPrice+p)"
        println("SobiTrader: making sell order: price=" + (b.tradingPrice + priceDelta) + ", volume=" + orderVolume)
        send(new LimitAskOrder(myId, baseOrderId, System.currentTimeMillis(), USD, USD, orderVolume, b.tradingPrice + priceDelta))
      }
    }

    case _ => println("SobiTrader: received unknown")
  }

  def start = {
    println("SobiTrader: Started")
    system.scheduler.schedule(0 milliseconds, intervalMillis milliseconds, self, FetchBooks)
  }

  /**
   * compute the volume-weighted average price of the top quartile*25% of the volume of the bids/asks orders book
   */
  def computeBiOrSi[T <: Order](bids: TreeSet[T]): Double = {
    if (bids.size > 4) {
      val it = bids.iterator
      var bi: Double = 0.0
      var vol: Double = 0
      for (i <- 0 to ((bids.size * quartile) / 4)) {
        val currentBidOrder = it.next()
        bi = bi + currentBidOrder.price * currentBidOrder.volume
        vol = vol + currentBidOrder.volume
      }
      bi / vol
    } else {
      0.0
    }
  }
}