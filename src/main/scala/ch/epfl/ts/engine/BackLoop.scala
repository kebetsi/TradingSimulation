package ch.epfl.ts.engine

import ch.epfl.ts.component.{ Component, StartSignal, StopSignal }
import ch.epfl.ts.component.persist.Persistance
import ch.epfl.ts.data.{ DelOrder, LimitAskOrder, LimitBidOrder, Transaction }
import akka.actor.Cancellable
import scala.concurrent.duration.DurationLong

case class OHLC(open: Double, high: Double, low: Double, close: Double, volume: Double, timestamp: Long, duration: Long)

/**
 * Backloop component, plugged as Market Simulator's output. Saves the transactions in a persistor.
 * distributes the transactions and delta orders to the trading agents
 */
class BackLoop(ohlcTimeFrameMillis: Int, p: Persistance[Transaction]) extends Component {
  import context._

  case class Tick()

  var schedule: Cancellable = null
  var values: List[Double] = Nil
  var volume: Double = 0.0
  var close: Double = 0.0

  override def receiver = {
    case StartSignal() => schedule = startScheduler
    case StopSignal()  => schedule.cancel()
    case Tick()        => send(computeOHLC)
    case t: Transaction => {
      p.save(t)
      send(t)
      values = t.price :: values
      volume = volume + t.volume
    }
    case la: LimitAskOrder => send(la)
    case lb: LimitBidOrder => send(lb)
    case d: DelOrder       => send(d)
    case _                 => println("Looper: received unknown")
  }

  /**
   * compute OHLC if values have been received, otherwise send OHLC with all values set to the close of the previous non-empty OHLC
   */
  private def computeOHLC: OHLC = {
    if (!values.isEmpty) {
      close = values.head
      val ret = OHLC(values.last, values.max(Ordering.Double), values.min(Ordering.Double), values.head, volume, System.currentTimeMillis(), ohlcTimeFrameMillis)
      // clean anciant vals
      volume = 0
      values = Nil
      println("BackLoop: sending OHLC : " + ret)
      ret
    } else {
      OHLC(close, close, close, close, 0, System.currentTimeMillis(), ohlcTimeFrameMillis)
    }
  }

  def startScheduler = context.system.scheduler.schedule(10 milliseconds, ohlcTimeFrameMillis milliseconds, self, new Tick)
}