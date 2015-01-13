package ch.epfl.ts.indicators

import ch.epfl.ts.component.Component
import akka.actor.Cancellable
import scala.concurrent.duration.DurationLong
import ch.epfl.ts.component.StartSignal
import ch.epfl.ts.component.StopSignal
import ch.epfl.ts.data.OHLC

case class SMA(override val value: Double, override val period: Int) extends MA(value, period)

class SmaIndicator(period: Int) extends MaIndicator(period) {

  def computeMa: SMA = {
    var sma: Double = 0.0
    values.map { o => sma = sma + o.close }
    SMA(sma / values.size, period)
  }
}