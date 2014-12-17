package ch.epfl.ts.component

import ch.epfl.ts.data.Transaction


case class Ohlc(value: Double)
class OhlcComponent extends Component {
  def receiver = {
    case Transaction => sender(new Ohlc(5.5))
  }
}
