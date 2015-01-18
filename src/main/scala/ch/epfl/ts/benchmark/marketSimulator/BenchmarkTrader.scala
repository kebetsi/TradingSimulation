package ch.epfl.ts.benchmark.marketSimulator

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.data.Currency._

class BenchmarkTrader extends Component {

  def receiver = {
    case t:Transaction => send(LastOrder(0L, 0L, 0L, BTC, USD, 0.0, 0.0))
    case _ =>
  }
}