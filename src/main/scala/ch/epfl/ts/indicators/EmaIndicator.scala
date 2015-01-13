package ch.epfl.ts.indicators

import ch.epfl.ts.component.Component
import akka.actor.Cancellable
import scala.concurrent.duration.DurationLong
import ch.epfl.ts.data.OHLC
import ch.epfl.ts.component.StartSignal
import ch.epfl.ts.component.StopSignal

case class EMA(override val value: Double, override val period: Int) extends MA(value: Double, period: Int)

class EmaIndicator(val period: Int) extends MaIndicator(period) {

  var previousEma: Double = 0.0

  def computeMa: EMA = {
    val alpha = 2 / (1 + 2 * period)
    previousEma = (alpha * values.head.close) + previousEma * (1 - alpha)
    EMA(previousEma, period)
  }

}