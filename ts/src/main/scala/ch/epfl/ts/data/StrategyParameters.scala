package ch.epfl.ts.data

import ch.epfl.ts.data.Currency.Currency
import scala.reflect.ClassTag
import scala.concurrent.duration.TimeUnit
import scala.concurrent.duration.{ MILLISECONDS => MillisecondsUnit }
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationLong


/**
 * Class behaving much like a map, designed to hold values
 * for a trading strategy's parameters.
 * Each parameter checks the validity of its value on instantiation.
 * 
 * It can be constructed with an arbitrary number of parameters.
 */
class StrategyParameters(params: (String, Parameter[_])*) {
  type Key = String
  val parameters = params.toMap
  
  /**
   * Similar to what a map's `get` would do.
   * We also perform type checking.
   */
  def get[T : ClassTag](key: Key): Option[T] = {
    parameters.get(key) match {
      case Some(p) => p.get() match {
        case v: T => Some(v)
        case _ => None
      }
      case _ => None
    }
  }
  
  /**
   * Get the value for this key
   * or fallback on the provided default if:
   *   - Such a key doesn't exist
   *   - The key exists but doesn't have the right parameter type
   */
  def getOrElse[T : ClassTag](key: Key, fallback: T): T =
    get[T](key) match {
      case Some(v) => v
      case _ => fallback
    }
  
  def getOrDefault[T : ClassTag](key: Key, parameterType: ParameterTrait[T]): T =
    getOrElse(key, parameterType.defaultValue)
  
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
 * If the designer of a strategy wishes to change any of the fields
 * in this trait, it is easy to extend it and override the field.
 */
trait ParameterTrait[T] {
  /** Make a new instance of the associated parameter */
  def getInstance(v: Any): Parameter[T]
  
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
  
  /**
   * Default value for this parameter
   */
  def defaultValue: T
  
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
  
  def defaultValue = 1.0
}



/**
 * Parameter representing a floating point coefficient in range [0; 1]
 */
case class NaturalNumberParameter(natural: Int) extends Parameter[Int]("NaturalNumber") {  
  def companion = NaturalNumberParameter
  def get(): Double = natural
}

object NaturalNumberParameter extends ParameterTrait[Int] {
  def getInstance(v: Int) = new NaturalNumberParameter(v)
  
  /**
   * Coefficient must lie in { 0, 1, ... }
   */
  def isValid(v: Int): Boolean = (v >= 0) 
  
  def validValues: Iterable[Int] = {
    // Lazily enumerates values from 0 to +inf
    val s: Stream[Int] = 0 #:: (s map (x => x + 1))
    s
  }
  
  def defaultValue = 0
}



/**
 * Parameter representing a duration.
 * 
 * @param duration
 * @param unit Defaults to milliseconds
 */
case class TimeParameter(duration: Long, unit: TimeUnit) extends Parameter[FiniteDuration]("Time") {
  def this(duration: Long) = this(duration, MillisecondsUnit)
	def this(duration: FiniteDuration) = this(duration.length, duration.unit)
  
  def companion = TimeParameter
  def get(): FiniteDuration = FiniteDuration(duration, unit)
}

object TimeParameter extends ParameterTrait[FiniteDuration] {
  import scala.language.postfixOps
  
  def getInstance(v: Long) = new TimeParameter(v)
  def getInstance(v: Long, u: TimeUnit) = new TimeParameter(v, u)
  def getInstance(d: FiniteDuration) = new TimeParameter(d)
  
  /**
   * Duration must be positive or null
   */
  def isValid(v: Long): Boolean = (v >= 0) 
  
  def validValues: Iterable[FiniteDuration] = {
    // Lazily enumerates values from 0 to +inf
    val s: Stream[FiniteDuration] = (0L milliseconds) #:: ( s map { d => FiniteDuration((d.length + 1), d.unit) } )
    s
  }
  
  def defaultValue = (0L milliseconds)
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
  
  def defaultValue = (Currency.EUR, Currency.CHF)
}