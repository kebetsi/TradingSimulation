package ch.epfl.ts.benchmark.marketSimulator

import ch.epfl.ts.component.{ Component, StartSignal }
import ch.epfl.ts.data.Order


class OrderFeeder(orders: List[Order]) extends Component {

  def receiver = {
    case StartSignal() => {
      orders.map { o => send(o) }
    }
    case _ => println("OrderFeeder: unknown received")
  }
}