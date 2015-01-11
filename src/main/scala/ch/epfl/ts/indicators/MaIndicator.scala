package ch.epfl.ts.indicators

import ch.epfl.ts.component.Component
import ch.epfl.ts.component.utils.OHLC
import akka.actor.Cancellable
import scala.concurrent.duration.DurationLong
import ch.epfl.ts.component.StartSignal
import ch.epfl.ts.component.StopSignal

abstract class MA(val value: Double, val period: Int)

abstract class MaIndicator(updatePeriodMillis: Int, period: Int) extends Component {
  import context._

  case class Tick()
  var schedule: Cancellable = null
  var values: List[OHLC] = Nil

  def receiver = {
    case StartSignal() => schedule = start
    case StopSignal()  => schedule.cancel()
    case o: OHLC       => values = o :: values
    case Tick()        => send(computeMa)
    case _             =>
  }
  
  def computeMa : MA

  private def start = system.scheduler.schedule(10 milliseconds, updatePeriodMillis milliseconds, self, Tick())
}