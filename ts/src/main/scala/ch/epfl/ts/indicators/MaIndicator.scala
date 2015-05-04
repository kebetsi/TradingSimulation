package ch.epfl.ts.indicators

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.OHLC
import ch.epfl.ts.data.Quote

import scala.collection.mutable.MutableList

/**
 * Moving Average value data
 */
abstract class MovingAverage(val value: Map[Int, Double])

/**
 * Moving average superclass. To implement a moving average indicator,
 * extend this class and implement the computeMa() method.
 */
abstract class MaIndicator(periods: List[Int]) extends Component {

  var values: MutableList[OHLC] = MutableList[OHLC]()
  val sortedPeriod = periods.sorted
  val maxPeriod = periods.last
  
  def receiver = {
    case o: OHLC => {
      println("MaIndicator: received OHLC: " + o)
      values += o
      if (values.size == maxPeriod) {
        val ma = computeMa
        println("MaIndicator: sending " + ma)
        send(ma)
        values = values.tail
      }
    } 
    case _ => println("MaIndicator : received unknown")
  }
  
  /**
   * Compute moving average
   */
  def computeMa : MovingAverage

}