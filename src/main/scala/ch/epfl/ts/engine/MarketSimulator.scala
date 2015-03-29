package ch.epfl.ts.engine

import scala.collection.mutable.{ HashMap => MHashMap }
import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Currency.Currency

/**
 * Base class for all market simulators.
 * Defines the interface and common behaviors.
 */
abstract class MarketSimulator(marketId: Long, rules: MarketRules) extends Component {
  
  /**
   * Format:
   *   (currency sold, currency bought) --> (bid price, ask price)
   */
  type Prices = MHashMap[(Currency, Currency), (Double, Double)]
  
  /**
   * Last price at which a transaction was executed for each currency.
   */
  // TODO: need to set initial trading price?
  var tradingPrices: Prices = MHashMap[(Currency, Currency), (Double, Double)]()
  
  val book = OrderBook(rules.bidsOrdering, rules.asksOrdering)
}