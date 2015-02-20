package ch.epfl.ts.indicators

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.OHLC

import scala.collection.mutable.MutableList

/**
 * Moving Average value data
 */
abstract class MA(val value: Double, val period: Int)

/**
 * Moving average superclass. To implement a moving average indicator,
 * extend this class and implement the computeMa() method.
 */
abstract class MaIndicator(period: Int) extends Component {

  var values: MutableList[OHLC] = MutableList[OHLC]()

  def receiver = {
    case o: OHLC       => {
      println("maIndicator: received ohlc: " + o)
      values += o //
      if (values.size == period) {
        val ma = computeMa
        println("maIndicator: sending " + ma)
        send(ma)
        values.clear()
      }
    }
    case _             =>
  }
  
  /**
   * compute moving average
   */
  def computeMa : MA

}