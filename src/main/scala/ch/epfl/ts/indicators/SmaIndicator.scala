package ch.epfl.ts.indicators

case class SMA(override val value: Double, override val period: Int) extends MA(value, period)

/**
 * Simple moving average indicator
 */
class SmaIndicator(period: Int) extends MaIndicator(period) {

  def computeMa: SMA = {
    var sma: Double = 0.0
    values.map { o => sma = sma + o.close }
    SMA(sma / values.size, period)
  }
}