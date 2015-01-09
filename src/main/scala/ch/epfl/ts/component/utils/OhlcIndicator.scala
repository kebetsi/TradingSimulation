package ch.epfl.ts.component.utils

import ch.epfl.ts.component.{ Component, StartSignal }
import scala.concurrent.duration.DurationInt
import ch.epfl.ts.data.Transaction

case class OHLC(open: Double, high: Double, low: Double, close: Double, volume: Double, timestamp: Long, duration: Long)

/**
 * this component computes OHLC for a timeframe provided in the constructor and distributes it to its destinators.
 */
class OhlcIndicator(duration: Int) extends Component {
  import context._

  case class Tick()

  var values: List[Double] = Nil
  var open: Double = 0.0
  var high: Double = 0.0
  var low: Double = 0.0
  var close: Double = 0.0
  var volume: Double = 0.0

  override def receiver = {

    case StartSignal() => start
    case t: Transaction => {
      values = t.price :: values
      volume = volume + t.volume
    }
    case this.Tick() => send(computeOHLC)
    case _           => println("OhlcIndicator: received unknown")
  }

  /**
   * compute OHLC if values have been received, otherwise send OHLC with all values set to the close of the previous non-empty OHLC
   */
  private def computeOHLC: OHLC = {
    if (!values.isEmpty) {
      val ret = OHLC(values.last, values.max(Ordering.Double), values.min(Ordering.Double), values.head, volume, System.currentTimeMillis(), duration)
      // clean anciant vals
      volume = 0
      values = Nil
      ret
    } else {
      OHLC(close, close, close, close, 0, System.currentTimeMillis(), duration)
    }
  }

  /**
   * start the scheduler
   */
  private def start = system.scheduler.schedule(0 milliseconds, duration milliseconds, self, Tick())
}