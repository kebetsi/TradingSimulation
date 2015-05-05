package ch.epfl.ts.indicators

case class SMA(override val value: Map[Int, Double]) extends MovingAverage(value)

/**
 * Simple moving average indicator
 */
class SmaIndicator(periods: List[Int]) extends MaIndicator(periods) {

  def computeMa: SMA = {

    def auxCompute(period: Int): Double = {
      var sma: Double = 0.0
      values.takeRight(period).map { o => sma = sma + o.close }
      sma / period
    }
    SMA(periods.map(p => (p -> auxCompute(p))).toMap)
  }
}