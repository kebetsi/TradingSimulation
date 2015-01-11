package ch.epfl.ts.indicators

import ch.epfl.ts.component.Component
import akka.actor.Cancellable
import scala.concurrent.duration.DurationLong
import ch.epfl.ts.component.StartSignal
import ch.epfl.ts.component.StopSignal
import ch.epfl.ts.engine.OHLC

case class SMA(override val value: Double, override val period: Int) extends MA(value, period)

class SmaIndicator(updatePeriodMillis: Int, period: Int) extends MaIndicator(updatePeriodMillis, period) {

  def computeMa: SMA = {
    var sma: Double = 0.0
    values = values.take(period)
    values.map { o => sma = sma + o.close }
    SMA(sma / values.size, period)
  }
}