package ch.epfl.ts.component

import ch.epfl.ts.data.Transaction


case class Ohlc(value: Double)
class OhlcComponent extends Component {
  override def receiver = {
    case Transaction => send(new Ohlc(5.5))
  }
}
