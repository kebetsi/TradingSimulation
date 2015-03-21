package ch.epfl.ts.engine

import scala.collection.mutable.{ HashMap => MHashMap }
import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Currency.Currency

/**
 * Base class for all market simulators.
 * Defines the interface and common behaviors.
 */
abstract class MarketSimulator extends Component {
  
  /**
   * Format:
   *   (currency sold, currency bought) --> price (in currency sold per unit of currency bought)
   */
  type Prices = MHashMap[(Currency, Currency), Double]
  
  /**
   * Last price at which a transaction was executed for each currency.
   */
  var tradingPrices: Prices = MHashMap[(Currency, Currency), Double]()
  
}