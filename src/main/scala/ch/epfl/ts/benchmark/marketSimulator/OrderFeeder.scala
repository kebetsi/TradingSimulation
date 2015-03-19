package ch.epfl.ts.benchmark.marketSimulator

import ch.epfl.ts.component.{Component, StartSignal}
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.Order

case class LastOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double) extends Order

/**
 * Component used to send orders to the MarketSimulator for the MarketSimulatorBenchmark.
 * It appends a LastOrder to the list of orders to send to notify the MarketSimulator
 * that there are no more orders to process.
 */
class OrderFeeder(orders: List[Order]) extends Component {

  def receiver = {
    case StartSignal() => {
      val ordersSent = orders :+ LastOrder(0L, 0L, System.currentTimeMillis(), DEF, DEF, 0.0, 0.0)
      send(StartSending(orders.size))
      ordersSent.map { o => send(o) }
    }
    case _ => println("OrderFeeder: unknown received")
  }
}