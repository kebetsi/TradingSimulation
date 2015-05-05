package ch.epfl.ts.data

import ch.epfl.ts.data.Currency.Currency
import scala.concurrent.duration.{ TimeUnit, FiniteDuration, DurationLong, MILLISECONDS => MillisecondsUnit }
import scala.reflect.ClassTag
import ch.epfl.ts.engine.MarketRules
import ch.epfl.ts.engine.ForexMarketRules
import ch.epfl.ts.benchmark.marketSimulator.BenchmarkMarketRules
import ch.epfl.ts.engine.Wallet


/**
 * Class behaving much like a map, designed to hold values
 * for a trading strategy's parameters.
 * Each parameter checks the validity of its value on instantiation.
 *
 * It can be constructed with an arbitrary number of parameters.
 */
class StrategyParameters(params: (String, Parameter)*) {
  type Key = String
  val parameters = params.toMap

  /**
   * @return True only if the key is available in `parameters`
   */
  def has(key: Key): Boolean = parameters.contains(key)
  /**
   * @return True only if the key is available in `parameters`
   *         AND it has the expected `Parameter` type
   */
  def hasWithType(key: Key, companion: ParameterTrait): Boolean =
    parameters.get(key) match {
      case Some(p) => (p.companion == companion)
      case _ => false
    }

  /**
   * Similar to what a map's `get` would do.
   * We also perform type checking on the type of the value held by the parameter.
   */
  def getOption[V: ClassTag](key: Key): Option[V] = {
    parameters.get(key) match {
      case Some(p) => p.value() match {
        case v: V => Some(v)
        case _ => None
      }
      case _ => None
    }
  }

  /**
   * Get the value if it is there, otherwise throw an exception.
   * Use it only if you are confident `params` contains the desired key.
   */
  def get[V: ClassTag](key: Key): V = getOption[V](key) match {
    case Some(v) => v
    case None => {
      val desired = implicitly[ClassTag[V]].runtimeClass
      throw new IndexOutOfBoundsException("Key `" + key + "` with desired type `" + desired + "` was not found.")
    }
  }

  /**
   * Get the value for this key
   * or fallback on the provided default if:
   *   - Such a key doesn't exist
   *   - The key exists but doesn't have the right parameter type
   */
  def getOrElse[V: ClassTag](key: Key, fallback: V): V =
    getOption[V](key) match {
      case Some(v) => v
      case _ => fallback
    }

  def getOrDefault[V: ClassTag](key: Key, parameterType: ParameterTrait{ type T = V }): V =
    getOrElse(key, parameterType.defaultValue)

  override def toString: String = {
    val strings = for {
      p <- parameters
      key = p._1
      paramType = p._2.name
      value = p._2.value().toString()
    } yield key + " (type " + paramType + ") = " + value

    strings.reduce((a, b) => a + '\n' + b)
  }
}



/**
 * Represents a generic trading strategy parameter.
 * Strategies can declare required and optional parameters.
 * Each parameter has a "range" of valid values.
 */
abstract class Parameter(val name: String) { self =>
  /** Type of the value this parameter holds */
  type T

  /**
   * At construction, ensure that the given value is legal for this parameter.
   */
  assert(isValid, "Illegal value " + value() + " for strategy parameter " + companion.name)

  /** Retrieve the value for this parameter */
  def value(): T

  /** The companion object of this parameter */
  def companion: ParameterTrait{ type T = self.T}

  /**
   * Whether or not this particular instance has been
   * parameterized with a legal value.
   */
  def isValid: Boolean = companion.isValid(value())

  override def toString: String = value() + " (type: " + companion.name + ")"
}



/**
 * "Static" methods that should be implemented in each concrete
 * parameter's companion object.
 * If the designer of a strategy wishes to change any of the fields
 * in this trait, it is easy to extend it and override the field.
 */
trait ParameterTrait { self =>
  type T

  /** Make a new instance of the associated parameter */
  def getInstance(v: T): Parameter{ type T = self.T}

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
 * Parameter representing a simple boolean flag (true / false)
 */
case class BooleanParameter(flag: Boolean) extends Parameter("BooleanFlag") {
  type T = Boolean
  def companion = BooleanParameter
  def value(): Boolean = flag
}

object BooleanParameter extends ParameterTrait {
  type T = Boolean
  def getInstance(v: Boolean) = new BooleanParameter(v)

  /** Any boolean value is valid */
  def isValid(v: Boolean): Boolean = true

  def validValues: Iterable[Boolean] = Seq(true, false)

  def defaultValue = false
}


/**
 * Parameter representing a floating point coefficient in range [0; 1]
 */
case class CoefficientParameter(coefficient: Double) extends Parameter("Coefficient") {
  type T = Double
  def companion = CoefficientParameter
  def value(): Double = coefficient
}

object CoefficientParameter extends ParameterTrait {
  type T = Double
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
 * Parameter representing an integer number greater or equal to zero.
 */
case class NaturalNumberParameter(natural: Int) extends Parameter("NaturalNumber") {
  type T = Int
  def companion = NaturalNumberParameter
  def value(): Int = natural
}

object NaturalNumberParameter extends ParameterTrait {
  type T = Int
  def getInstance(v: Int) = new NaturalNumberParameter(v)

  /**
   * Coefficient must lie in { 0, 1, ... }
   */
  def isValid(v: Int): Boolean = (v >= 0)

  def validValues: Iterable[Int] = Stream.from(0)

  def defaultValue = 0
}


/**
 * Parameter representing an real number greater of any sign.
 */
case class RealNumberParameter(real: Double) extends Parameter("RealNumber") {
  type T = Double
  def companion = RealNumberParameter
  def value(): Double = real
}

object RealNumberParameter extends ParameterTrait {
  type T = Double
  def getInstance(v: Double) = new RealNumberParameter(v)

  /**
   * Any floating point number is valid
   */
  def isValid(v: Double): Boolean = true

  // TODO: how should we enumerate all Reals?
  def validValues: Iterable[Double] = Stream.from(0).map(x => x / 100.0)

  def defaultValue = 0.0
}



/**
 * Parameter representing a duration.
 *
 * @param duration
 * @param unit Defaults to milliseconds
 */
case class TimeParameter(duration: Long, unit: TimeUnit) extends Parameter("Time") {
  type T = FiniteDuration
  def this(duration: Long) = this(duration, MillisecondsUnit)
  def this(duration: FiniteDuration) = this(duration.length.toLong, duration.unit)

  def companion = TimeParameter
  def value(): FiniteDuration = FiniteDuration(duration, unit)
}

object TimeParameter extends ParameterTrait {
  import scala.language.postfixOps

  type T = FiniteDuration

  def getInstance(v: Long) = new TimeParameter(v)
  def getInstance(v: Long, u: TimeUnit) = new TimeParameter(v, u)
  def getInstance(d: FiniteDuration) = new TimeParameter(d)

  /**
   * Duration must be positive or null
   */
  def isValid(v: Long): Boolean = (v >= 0)
  def isValid(d: FiniteDuration): Boolean = (d.length >= 0)

  // TODO: user-selected resolution
  def validValues: Iterable[FiniteDuration] =
    Stream.from(0) map { n => (100L * n) milliseconds }

  def defaultValue = (0L milliseconds)
}



/**
 * Parameter representing a pair of currencies to be traded.
 */
case class CurrencyPairParameter(currencies: (Currency, Currency)) extends Parameter("CurrencyPair") {
  type T = (Currency, Currency)
  def companion = CurrencyPairParameter
  def value(): (Currency, Currency) = currencies
}

object CurrencyPairParameter extends ParameterTrait {
  type T = (Currency, Currency)
  def getInstance(v: T) = new CurrencyPairParameter(v)

  /**
   * All currency pairs are acceptable as long as they're not twice the same.
   * @warning Note that (EUR, CHF) is *not* the same pair as (CHF, EUR)
   */
  def isValid(v: T): Boolean = (v._1 != v._2)

  def validValues: Iterable[T] = {
    val allCurrencies = Currency.supportedCurrencies()

    for {
      c1 <- allCurrencies
      c2 <- allCurrencies
      if c1 != c2
    } yield (c1, c2)
  }

  def defaultValue = (Currency.EUR, Currency.CHF)
}


/**
 * Parameter representing funds in various currencies.
 */
case class WalletParameter(wallet: Wallet.Type) extends Parameter("Wallet") {
  type T = Wallet.Type
  def companion = WalletParameter
  def value(): T = wallet
}

object WalletParameter extends ParameterTrait {
  type T = Wallet.Type
  def getInstance(v: T) = new WalletParameter(v)

  /**
   * All amounts must be non-negative
   */
  def isValid(v: T): Boolean = v.values.forall { x => x >= 0 }

  // TODO: enumeration of valid wallets
  def validValues: Iterable[T] = Seq(defaultValue)

  def defaultValue = Map[Currency, Double]()
}


/**
 * Parameter representing rules applied in a certain market.
 */
case class MarketRulesParameter(rules: MarketRules) extends Parameter("MarketRules") {
  type T = MarketRules
  def companion = MarketRulesParameter
  def value(): MarketRules = rules
}

object MarketRulesParameter extends ParameterTrait {
  type T = MarketRules
  def getInstance(v: T) = new MarketRulesParameter(v)

  def isValid(v: T): Boolean = true

  def validValues: Iterable[T] = List(new MarketRules, new ForexMarketRules, new BenchmarkMarketRules)

  def defaultValue = new MarketRules
}
