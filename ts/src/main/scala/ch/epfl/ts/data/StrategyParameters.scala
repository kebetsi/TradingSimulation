package ch.epfl.ts.data

import ch.epfl.ts.data.Currency.Currency


/**
 * Class behaving much like a map, designed to hold values
 * for a trading strategy's parameters.
 * When given a parameter and its value, it checks that the value
 * is within the parameter's acceptable range.
 */
class StrategyParameters {
  /**
   * Constructor which takes an arbitrary number of Parameter -> Value pairs
   * (much like a map).
   */
   // TODO
}

abstract class Parameter(val name: String) {
  override def toString: String = name + "Parameter"
  
  // TODO: other common fields and methods (valid range, parameters generation, etc)
}

case class CurrencyParameter(currency: Currency) extends Parameter("Currency")