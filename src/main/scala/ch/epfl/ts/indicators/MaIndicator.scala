package ch.epfl.ts.indicators

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.OHLC

import scala.collection.mutable.MutableList

abstract class MA(val value: Double, val period: Int)

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