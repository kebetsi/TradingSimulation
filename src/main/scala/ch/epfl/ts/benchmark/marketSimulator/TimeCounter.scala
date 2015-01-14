package ch.epfl.ts.benchmark.marketSimulator

import ch.epfl.ts.component.Component

case class StartSending()
case class FinishedProcessingOrders()

class TimeCounter extends Component {

  var initSendingTime: Long = 0L

  def receiver = {
    case StartSending()             =>
      initSendingTime = System.currentTimeMillis(); println("TimeCounter: received StartSending from " + sender)
    case FinishedProcessingOrders() => println("TimeCounter: processed orders in " + (System.currentTimeMillis() - initSendingTime) + " ms.")
    case _                          =>
  }
}