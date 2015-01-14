package ch.epfl.ts.benchmark.marketSimulator

import ch.epfl.ts.component.{ Component, StartSignal }
import ch.epfl.ts.data.{Order, Currency}
import ch.epfl.ts.data.Currency._

case class LastOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double) extends Order(oid, uid, timestamp, whatC, withC, volume, price)

class OrderFeeder(orders: List[Order]) extends Component {

  def receiver = {
    case StartSignal() => {
      val ordersSent = orders :+ LastOrder(0L, 0L, System.currentTimeMillis(), DEF, DEF, 0.0, 0.0)
      send(StartSending())
      ordersSent.map { o => send(o) }
    }
    case _ => println("OrderFeeder: unknown received")
  }
}