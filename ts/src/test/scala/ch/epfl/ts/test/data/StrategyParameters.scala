package ch.epfl.ts.test.data

import scala.language.postfixOps
import scala.util.Try
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import scala.concurrent.duration.DurationLong
import ch.epfl.ts.data.CoefficientParameter
import ch.epfl.ts.data.Currency
import ch.epfl.ts.data.CurrencyPairParameter
import ch.epfl.ts.data.NaturalNumberParameter
import ch.epfl.ts.data.Parameter
import ch.epfl.ts.data.ParameterTrait
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.data.TimeParameter
import ch.epfl.ts.traders.TraderCompanion
import ch.epfl.ts.traders.MadTrader
import ch.epfl.ts.data.StrategyParameters

@RunWith(classOf[JUnitRunner])
class StrategyParametersTests extends FunSuite {
  /**
   * Common test values
   */
	val currencies1 = (Currency.EUR, Currency.CHF)
	val currencies2 = (Currency.GBP, Currency.USD)
  val coefficient1 = 0.2342341341
  val coefficient2 = -0.2342341341
  
  private type CurrencyPair = (Currency.Currency, Currency.Currency)
  
  /** Generic function to test basic functionality expected from Parameter subclasses */
  def testConcreteParameter[V](parameterTrait: ParameterTrait{ type T = V}, validValues: Iterable[V], invalidValues: Iterable[V]) = {
    
    test(parameterTrait + " should give back the value it was instantiated with") {
      for {
        myValue <- validValues
        parameter = parameterTrait.getInstance(myValue)        
      } yield assert(parameter.get() == myValue, "Should give back value " + myValue)
    }
    
    test(parameterTrait + " should reject invalid values") {
      for {
        myValue <- invalidValues
        attempt = Try(parameterTrait.getInstance(myValue))
      } yield assert(attempt.isFailure, "Should fail with illegal value " + myValue)
    }
  
    test(parameterTrait + " should accept the 100 first values it declares as valid values") {
      for {
        value <- (parameterTrait.validValues take 100).toList
        attempt = Try(parameterTrait.getInstance(value))
      } yield assert(attempt.isSuccess, "Should accept " + value)
    }
    
    test(parameterTrait + " should have a default value which is valid") {
    	val attempt = Try(parameterTrait.getInstance(parameterTrait.defaultValue))
    	assert(attempt.isSuccess, "Should accept " + parameterTrait.defaultValue)
    }
  }
  
  
  /**
   * Generic function to test concrete trading strategy implementation's correct
   * behavior when instantiated with correct & incorrect parameters
   */
  def testConcreteStrategy(strategyCompanion: TraderCompanion) = {
    def make(p: StrategyParameters) = strategyCompanion.getInstance(42, p)
    val emptyParameters = new StrategyParameters()
    val required = strategyCompanion.requiredParameters
    val optonal = strategyCompanion.optionalParameters
    
    
    /**
     * Strategies not having any required parameter
     */
    if(required.isEmpty) {
      test(strategyCompanion + " should allow instantiation with no parameters") {
        val attempt = Try(make(emptyParameters))
        assert(attempt.isSuccess)
      }
    }
    /**
     * Strategies with required parameters
     */
    else {
    	test(strategyCompanion + " should not allow instantiation with no parameters") {
    		val attempt = Try(make(emptyParameters))
    		assert(attempt.isFailure)
    	}
      
      test(strategyCompanion + " should allow instantiation with parameters' default values") {
        val parameters = for {
          pair <- strategyCompanion.requiredParameters.toSeq
          key = pair._1
          parameter = pair._2.getInstance(pair._2.defaultValue)
        } yield (key, parameter)
        
        val sp = new StrategyParameters(parameters: _*)
        val attempt = Try(make(sp))
        assert(attempt.isSuccess)
      }
    }
  }
  
  test("Should allow to add several parameters at a time") {
    val myParameters = new StrategyParameters(
      "tradedCurrencies" -> CurrencyPairParameter(currencies1),
      "someCoefficient" -> CoefficientParameter(coefficient1),
      "someOtherParameter" -> CurrencyPairParameter(currencies2)
    )
  }
  
  test("Should fail at instantiation with illegal parameters") {
	  val attempt = Try(new StrategyParameters("someCoefficient" -> CoefficientParameter(coefficient2)))
    assert(attempt.isFailure, "Should fail to instantiate a coefficient with value " + coefficient2)
  }
  
  test("Should hold the parameters and yield back their values") {
	  val myParameters = new StrategyParameters(
      "tradedCurrencies" -> CurrencyPairParameter(currencies1),
      "someCoefficient" -> CoefficientParameter(coefficient1)
    )
    
    assert(myParameters.get[CurrencyPair]("tradedCurrencies") == currencies1)
    assert(myParameters.get[Double]("someCoefficient") == coefficient1)
  }
  
  test("Should not say it has a value if it doesn't have the expected type") {
    val myParameters = new StrategyParameters("tradedCurrencies" -> CoefficientParameter(coefficient1))
    assert(!myParameters.hasWithType("tradedCurrencies", CurrencyPairParameter), "Should not allow for the wrong type to be retrieved")
  }
  test("Should say it has a value if we don't care about the type") {
	  val myParameters = new StrategyParameters("tradedCurrencies" -> CoefficientParameter(coefficient1))
	  assert(myParameters.has("tradedCurrencies"), "Should allow for another type to match")
  }
  
  test("Should not yield a value if it doesn't have the expected type") {
	  val myParameters = new StrategyParameters("tradedCurrencies" -> CoefficientParameter(coefficient1))
	  
	  val got = myParameters.getOption[CurrencyPair]("tradedCurrencies")
	  assert(got == None, "Should not allow for the wrong type to be retrieved")
  }
  
  test("Should fallback on default if allowed") {
	  val myParameters = new StrategyParameters()
	  
	  val got1 = myParameters.getOrDefault("tradedCurrencies", CurrencyPairParameter)
	  val got2 = myParameters.getOrDefault("someCoefficient", CoefficientParameter)
	  assert(got1 == CurrencyPairParameter.defaultValue)
	  assert(got2 == CoefficientParameter.defaultValue)
  }
  
  testConcreteParameter(
      CoefficientParameter,
      List(0.058924379237491379, 0.0, 1.0),
      List(-1.0, -0.0001, 1.000001)
    )
  
  testConcreteParameter(
      NaturalNumberParameter,
      List(0, 10, 1000, 1337),
      List(-1, -10, -1337)
    )
    
  testConcreteParameter(
  		TimeParameter,
  		List(0L hours, 10L seconds, 1L milliseconds, 1337L milliseconds),
  		List(-1L seconds, -1000L milliseconds)
  	)
    
  testConcreteParameter(
      CurrencyPairParameter,
      List((Currency.EUR, Currency.CHF), (Currency.GBP, Currency.USD)),
      List((Currency.EUR, Currency.EUR), (Currency.CHF, Currency.CHF))
    )


  /** Simple tests for strategy's parameterization */
  testConcreteStrategy(MadTrader)
    
}