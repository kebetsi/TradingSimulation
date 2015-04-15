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
  def testConcreteParameter[T](parameterTrait: ParameterTrait[T], validValues: Iterable[T], invalidValues: Iterable[T]) = {
    
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
  
    test(parameterTrait + " should accept all values within the acceptable range") {
      for {
        value <- parameterTrait.validValues
        attempt = Try(parameterTrait.getInstance(value))
      } yield assert(attempt.isSuccess, "Should accept " + value)
    }
    
    test(parameterTrait + " should have a default value which is valid") {
    	val attempt = Try(parameterTrait.getInstance(parameterTrait.defaultValue))
    	assert(attempt.isSuccess, "Should accept " + parameterTrait.defaultValue)
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
    
    assert(myParameters.get[CurrencyPair]("tradedCurrencies") == Some(currencies1))
    assert(myParameters.get[Double]("someCoefficient") == Some(coefficient1))
  }
  
  test("Should not yield a value if it doesn't have the expected type") {
    val myParameters = new StrategyParameters("tradedCurrencies" -> CoefficientParameter(coefficient1))
    
    val got = myParameters.get[CurrencyPair]("tradedCurrencies")
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
}