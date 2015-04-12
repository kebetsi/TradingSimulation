package ch.epfl.ts.indicators

case class EMA(override val value: Double, override val period: Int) extends MA(value: Double, period: Int)

/**
 * Exponentially weighted moving average component.
 */
class EmaIndicator(val period: Int) extends MaIndicator(period) {

  var previousEma: Double = 0.0

  def computeMa: EMA = {
    val alpha = 2 / (1 + 2 * period)
    previousEma = (alpha * values.head.close) + previousEma * (1 - alpha)
    EMA(previousEma, period)
  }

}