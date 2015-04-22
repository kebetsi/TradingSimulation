package ch.epfl.ts.traders

import ch.epfl.ts.component.{Component, ComponentRef}
import ch.epfl.ts.data._
import ch.epfl.ts.data.Currency._

/**
 * Implements back testing for traders.
 *
 * To use this class, redirect all previous connections into trader to instances
 * of this class. Connections out of the original trader should remain unchanged.
 *
 * @param traderId the id of the trader
 * @param trader the reference to the real trader component
 */
class BacktestTrader(traderId: Long, wallet: Map[Currency, Double], trader: ComponentRef) extends Component {
  def returns: Double = ???
  def volatility: Double = ???
  def drawdown: Double = ???
  def sharp: Double = ???

  override def receiver = {
    case t: Transaction if t.buyerId == traderId =>  // buy
      trader.ar ! t
    case t: Transaction if t.sellerId == traderId => // sell
      trader.ar ! t
    case m => trader.ar ! m
  }
}
