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

/**
 * Represents a generic trading strategy parameter.
 * Strategies can declare required and optional parameters.
 * Each parameter has a "range" of valid values.
 * @param T Parameter value type
 */
abstract class Parameter[T](val name: String) {
  /**
   * At construction, ensure that the given value is legal for this parameter.
   */
  assert(isValid, "Illegal value " + get() + " for strategy parameter " + toString)
  
  /** Retrieve the value for this parameter */
  def get(): T
  
  /** The companion object of this parameter */
  protected val companion: ParameterTrait[T]
  
  /**
   * Whether or not this particle instance has been
   * parameterized with a legal value.
   */
  def isValid: Boolean = companion.isValid(get())
  
  override def toString: String = name + "Parameter"
}

/**
 * "Static" methods that should be implemented in each concrete
 * parameter's companion object.
 */
trait ParameterTrait[T] {
  
  /**
   * Should be implemented in each concrete strategy parameter
   * @return Whether the value is suitable for this parameter
   */
  def isValid(value: T): Boolean

  /**
   * Generate the range of valid values for this parameter.
   * TODO: How to handle infinite ranges? Use streams?
   */
  def validValues: Iterable[T]
}

case class CurrencyParameter(currency: Currency) extends Parameter("Currency")