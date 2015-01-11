package ch.epfl.ts.indicators

import ch.epfl.ts.component.Component
import akka.actor.Cancellable
import scala.concurrent.duration.DurationLong
import ch.epfl.ts.component.utils.OHLC
import ch.epfl.ts.component.StartSignal
import ch.epfl.ts.component.StopSignal

case class EMA(override val value: Double, override val period: Int) extends MA(value: Double, period: Int)

class EmaIndicator(val updatePeriodMillis: Int, val period: Int) extends MaIndicator(updatePeriodMillis, period) {
  import context._

  var previousEma: Double = 0.0
  var periods: Int = 0

  def computeMa: EMA = {
    val alpha = 2 / (1 + 2 * periods)
    previousEma = (alpha * values.head.close) + previousEma * (1 - alpha)
    EMA(previousEma, period)
  }

}