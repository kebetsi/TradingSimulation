package ch.epfl.ts.component.utils

import ch.epfl.ts.component.{Component, StartSignal}

import scala.concurrent.duration.DurationInt

case class OHLC(open: Double, high: Double, low: Double, close: Double)

/**
 * this component computes OHLC for a timeframe provided in the constructor and distributes it to its destinators.
 */
class OhlcIndicator(timeFrameMillis: Int) extends Component {
  import context._

  case class Tick()

  var values: List[Double] = Nil
  var open: Double = 0.0
  var high: Double = 0.0
  var low: Double = 0.0
  var close: Double = 0.0

  override def receiver = {

    case StartSignal() => start
    case v: Double     => values = v :: values
    case this.Tick()   => send(computeOHLC)
    case _             => println("OhlcIndicator: received unknown")
  }

  /**
   * compute OHLC if values have been received, otherwise send OHLC with all values set to the close of the previous non-empty OHLC
   */
  private def computeOHLC: OHLC = {
    if (!values.isEmpty) {
      open = values.last
      close = values.head
      high = values.max(Ordering.Double)
      low = values.min(Ordering.Double)
      // clean anciant vals
      values = Nil
      OHLC(open, high, low, close)
    } else {
      OHLC(close, close, close, close)
    }
  }

  /**
   * start the scheduler
   */
  private def start = system.scheduler.schedule(0 milliseconds, timeFrameMillis milliseconds, self, Tick())
}