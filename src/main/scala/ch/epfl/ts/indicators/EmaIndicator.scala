package ch.epfl.ts.indicators

import ch.epfl.ts.component.Component
import akka.actor.Cancellable
import scala.concurrent.duration.DurationLong
import ch.epfl.ts.component.utils.OHLC
import ch.epfl.ts.component.StartSignal
import ch.epfl.ts.component.StopSignal

case class EMA(value: Double, timeframe: Int)

class EmaIndicator(timeFrameMillis: Int) extends Component {
  import context._

  case class Tick()
  var schedule: Cancellable = null
  var values: List[OHLC] = Nil
  var previousEma: Double = 0.0
  var periods: Int = 0

  def receiver = {
    //    case StartSignal() => schedule = start
    case StopSignal() => schedule.cancel()
    case o: OHLC => {
      values = o :: values
      periods = periods + 1
      send(computeEMA(o))
    }
    //    case Tick() => 
    case _ =>
  }

  def computeEMA(ohlc: OHLC): EMA = {
    val alpha = 2 / (1 + 2 * periods)
    previousEma = (alpha * ohlc.close) + previousEma * (1 - alpha)
    EMA(previousEma, timeFrameMillis)
  }

  //  private def start = system.scheduler.schedule(10 milliseconds, timeFrameMillis milliseconds, self, Tick())

}