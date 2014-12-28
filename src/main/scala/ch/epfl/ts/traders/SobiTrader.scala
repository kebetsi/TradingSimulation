package ch.epfl.ts.traders

import akka.actor.Actor
import akka.actor.ActorRef
import ch.epfl.ts.component.Component
import scala.concurrent.duration.DurationInt
import ch.epfl.ts.engine.RetrieveBooks
import ch.epfl.ts.engine.Books
import scala.collection.mutable.PriorityQueue
import ch.epfl.ts.engine.EngineOrder
import ch.epfl.ts.engine.BidOrder
import ch.epfl.ts.engine.AskOrder
import ch.epfl.ts.data.Currency._

case class FetchBooks()

class SobiTrader(market: ActorRef, intervalMillis: Int, quartile: Int, theta: Double, orderVolume: Int, priceDelta: Double)
  extends Component {
  import context._
  system.scheduler.schedule(0 milliseconds, intervalMillis milliseconds, self, FetchBooks)

  var bi: Double = 0.0
  var si: Double = 0.0
  val myId = 123
  var baseOrderId: Long = 456789

  def receiver = {
    case FetchBooks => {
      market ! RetrieveBooks
    }
    case b: Books => {
      bi = computeBiOrSi(b.bids)
      si = computeBiOrSi(b.asks)
      if ((si - bi) > 200) {
        baseOrderId = baseOrderId + 1
        //"place an order to buy x shares at (lastPrice-p)"
        println("SobiTrader: making buy order: price=" + (b.tradingPrice - priceDelta) + ", volume=" + orderVolume)
        market ! BidOrder(myId, baseOrderId, System.currentTimeMillis, USD, b.tradingPrice - priceDelta, orderVolume, USD)
      }
      if ((bi - si) > theta) {
        baseOrderId = baseOrderId + 1
        //"place an order to sell x shares at (lastPrice+p)"
        println("SobiTrader: making sell order: price=" + (b.tradingPrice + priceDelta) + ", volume=" + orderVolume)
        market ! AskOrder(myId, baseOrderId, System.currentTimeMillis(), USD, b.tradingPrice + priceDelta, orderVolume, USD)
      }
    }
  }

  /**
   * compute the volume-weighted average price of the top quartile*25% of the volume of the bids/asks orders book
   */
  def computeBiOrSi[T <: EngineOrder](bids: PriorityQueue[T]): Double = {
    if (bids.size > 4) {
      val it = bids.iterator
      var bi: Double = 0.0
      var vol: Double = 0
      for (i <- 0 to ((bids.size * quartile) / 4)) {
        val currentBidOrder = it.next()
        bi = bi + currentBidOrder.price * currentBidOrder.quantity
        vol = vol + currentBidOrder.quantity
      }
      bi / vol
    } else {
      0.0
    }
  }

}