package ch.epfl.ts.benchmark.marketSimulator

import ch.epfl.ts.component.Component

case class StartSending(ordersCount: Int)
case class FinishedProcessingOrders(asksSize: Int, bidsSize: Int)

/**
 * Simple component used to compute the time it takes for the MarketSimulatorBenchmark
 * to process the orders. It receives a start and stop signal, computes the time
 * difference and prints the result.
 */
class TimeCounter extends Component {

  var initSendingTime: Long = 0L
  var ordersCount = 0

  def receiver = {
    case StartSending(o) => {
      ordersCount = o
      initSendingTime = System.currentTimeMillis(); println("TimeCounter: feeding " + o + " orders started.")
    }
    case FinishedProcessingOrders(aSize, bSize) => {
      println("TimeCounter: processed " + ordersCount + " orders in " + (System.currentTimeMillis() - initSendingTime) + " ms.")
      println("TimeCounter: askOrdersBook size = " + aSize + ", bidOrdersBook size = " + bSize)
    }
    case _ =>
  }
}