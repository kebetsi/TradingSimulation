package ch.epfl.ts.indicators

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.{ OHLC, Transaction, Quote }
import ch.epfl.ts.data.Currency
import ch.epfl.ts.data.Currency.Currency
import scala.collection.mutable.MutableList

/**
 * computes OHLC tick for a tick frame of the provided size, the OHLCs are identified with the provided marketId
 */
class OhlcIndicator(marketId: Long, symbol: (Currency,Currency), tickSizeMillis: Long) extends Component {

  /**
   * stores transactions' price values
   */
  var values: MutableList[Double] = MutableList[Double]()
  var volume: Double = 0.0
  var close: Double = 0.0
  var currentTick: Long = 0
  val (whatC, withC) = symbol

  override def receiver = {
    
    //We either receive the price from quote (Backtesting/realtime trading) or from transaction (for simulation)
    
    case t: Transaction => {
      if (whichTick(t.timestamp) > currentTick) {
        // new tick, send OHLC with values stored until now, and reset accumulators (Transaction volume & prices)
        send(computeOHLC)
        currentTick = whichTick(t.timestamp)
      }
      values += t.price
      volume = volume + t.volume
    }

    case q: Quote => {
      if(currentTick==0){
         currentTick =whichTick(q.timestamp)
      }
      if (q.whatC == whatC && q.withC == withC) {
        println("OhlcIndicator : recieved quote" + q.timestamp)
        if (whichTick(q.timestamp) > currentTick) {
          send(computeOHLC)
          currentTick = whichTick(q.timestamp)
        }
        values += q.bid //we consider the price as the bid price
      }
    }
    case _ => println("OhlcIndicator: received unknown")
  }

  /**
   * computes to which tick an item with the provided timestamp belongs to
   */
  private def whichTick(timestampMillis: Long): Long = {
    timestampMillis / tickSizeMillis
  }

  /**
   * compute OHLC if values have been received, otherwise send OHLC with all values set to the close of the previous non-empty OHLC
   */
  private def computeOHLC: OHLC = {
    // set OHLC's timestamp
    val tickTimeStamp = currentTick * tickSizeMillis

    if (!values.isEmpty) {
      close = values.head
      val ohlc = OHLC(marketId, values.last, values.max(Ordering.Double), values.min(Ordering.Double), values.head, volume, tickTimeStamp, tickSizeMillis)
      // clean ancient values
      volume = 0
      values.clear()
      println("OhlcIndicator: sending OHLC : " + ohlc)
      ohlc
    } else {
      OHLC(marketId, close, close, close, close, 0, tickTimeStamp, tickSizeMillis)
    }
  }
}