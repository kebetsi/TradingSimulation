package ch.epfl.ts.benchmark.marketSimulator

import ch.epfl.ts.component.Component

case class StartSending(ordersCount: Int)
case class FinishedProcessingOrders(asksSize: Int, bidsSize: Int)

class TimeCounter extends Component {

  var initSendingTime: Long = 0L
  var ordersCount = 0

  def receiver = {
    case StartSending(o) => {
      ordersCount = o
      initSendingTime = System.currentTimeMillis(); println("TimeCounter: received StartSending from " + sender)
    }
    case FinishedProcessingOrders(aSize, bSize) => {
      println("TimeCounter: processed " + ordersCount + " orders in " + (System.currentTimeMillis() - initSendingTime) + " ms.")
      println("TimeCounter: askOrdersBook Size = " + aSize + ", bidOrdersBook size = " + bSize)
    }
    case _ =>
  }
}