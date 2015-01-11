package ch.epfl.ts.indicators

import ch.epfl.ts.component.Component
import ch.epfl.ts.component.utils.OHLC
import akka.actor.Cancellable
import scala.concurrent.duration.DurationLong
import ch.epfl.ts.component.StartSignal
import ch.epfl.ts.component.StopSignal

case class SMA(value: Double, timeframe: Int)

class SmaIndicator(timeFrameMillis: Int) extends Component {
  import context._

  case class Tick()
  var schedule: Cancellable = null
  var values: List[OHLC] = Nil

  def receiver = {
    case StartSignal() => schedule = start
    case StopSignal()  => schedule.cancel()
    case o: OHLC       => values = o :: values
    case Tick()        => send(computeSMA)
    case _             =>
  }

  def computeSMA: SMA = {
    var sma: Double = 0.0
    values.map { o => sma = sma + o.close }
    SMA(sma / values.size, timeFrameMillis)
  }

  private def start = system.scheduler.schedule(10 milliseconds, timeFrameMillis milliseconds, self, Tick())
}