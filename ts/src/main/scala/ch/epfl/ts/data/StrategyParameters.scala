package ch.epfl.ts.data

import ch.epfl.ts.data.Currency.Currency


/**
 * Class behaving much like a map, designed to hold values
 * for a trading strategy's parameters.
 * Each parameter checks the validity of its value on instantiation.
 * 
 * It can be constructed with an arbitrary number of parameters.
 */
class StrategyParameters(val parameters: (String, Parameter[_])*) {
  
  // TODO: mechanism to get parameter value with automatic fallback on a default value
  
  override def toString: String = {
    val strings = (for {
      p <- parameters
      key = p._1
      paramType = p._2.toString()
      value = p._2.get().toString()
    } yield key + " (type " + paramType + ") = " + value).toList
    
    strings.reduce((a, b) => a + '\n' + b)
  }
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
  assert(isValid, "Illegal value " + get() + " for strategy parameter " + companion.name)
  
  /** Retrieve the value for this parameter */
  def get(): T
  
  /** The companion object of this parameter */
  protected def companion: ParameterTrait[T]
  
  /**
   * Whether or not this particle instance has been
   * parameterized with a legal value.
   */
  def isValid: Boolean = companion.isValid(get())
  
  override def toString: String = get() + " (type: " + companion.name + ")"
}



/**
 * "Static" methods that should be implemented in each concrete
 * parameter's companion object.
 */
trait ParameterTrait[T] {
  /** Make a new instance of the associated parameter */
  def getInstance(v: T): Parameter[T]
  
  /** Name of this parameter type */
  def name: String = this.getClass.getName
  
  /**
   * @return Whether the value is suitable for this parameter
   */
  def isValid(value: T): Boolean

  /**
   * Generate the range of valid values for this parameter.
   * TODO: How to handle infinite ranges? Use streams?
   */
  def validValues: Iterable[T]
  
  override def toString = name
}



/**
 * Parameter representing a floating point coefficient in range [0; 1]
 */
case class CoefficientParameter(coefficient: Double) extends Parameter[Double]("Coefficient") {  
  def companion = CoefficientParameter
  def get(): Double = coefficient
}

object CoefficientParameter extends ParameterTrait[Double] {
  def getInstance(v: Double) = new CoefficientParameter(v)
  
  /**
   * Coefficient must lie in [0; 1]
   */
  def isValid(v: Double): Boolean = (v >= 0.0) && (v <= 1.0) 
  
  // TODO: handle user-selected resolution for these values
  def validValues: Iterable[Double] = {
    val resolution = 0.01
    for {
      n <- 0 to (1 / resolution).toInt
    } yield (n * resolution)
  }
}



/**
 * Parameter representing a pair of currencies to be traded.
 */
case class CurrencyPairParameter(currencies: (Currency, Currency)) extends Parameter[(Currency, Currency)]("CurrencyPair") {  
	def companion = CurrencyPairParameter
	def get(): (Currency, Currency) = currencies
}

object CurrencyPairParameter extends ParameterTrait[(Currency, Currency)] {
	private type CurrencyPair = Tuple2[Currency, Currency]
	def getInstance(v: CurrencyPair) = new CurrencyPairParameter(v)
  
	/**
	 * All currency pairs are acceptable as long as they're not twice the same.
	 * @TODO: It would be great if we could enforce an order inside the pair, so that we avoid having to handle swapped currency pairs
	 */
	def isValid(v: CurrencyPair): Boolean = (v._1 != v._2)
	
	def validValues: Iterable[CurrencyPair] = {
			val allCurrencies = Currency.supportedCurrencies()
					
					// TODO: this leads to "duplicate" pairs, e.g. (EUR, CHF) and (CHF, EUR). Is this desirable?
					for {
						c1 <- allCurrencies
						c2 <- allCurrencies
						if c1 != c2
					} yield (c1, c2)
	}
}