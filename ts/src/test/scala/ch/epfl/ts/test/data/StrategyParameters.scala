package ch.epfl.ts.test.data

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import ch.epfl.ts.data.Currency
import scala.util.Try
import ch.epfl.ts.data.CurrencyPairParameter
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.data.CoefficientParameter
import ch.epfl.ts.data.Parameter
import ch.epfl.ts.data.ParameterTrait

@RunWith(classOf[JUnitRunner])
class StrategyParametersTests extends FunSuite {
  /**
   * Common test values
   */
  val legalCurrencies1 = (Currency.EUR, Currency.CHF)
  val legalCurrencies2 = (Currency.GBP, Currency.USD)
  val illlegalCurrencies = (Currency.GBP, Currency.GBP)
  val legalCoefficient = 0.2342341341
  val illegalCoefficient = -0.2342341341
  
  private type CurrencyPair = (Currency.Currency, Currency.Currency)
  
  /** Generic function to test basic functionality expected from Parameter subclasses */
  def testConcreteParameter[T](parameterTrait: ParameterTrait[T], validValues: Iterable[T], invalidValues: Iterable[T]) = {
    test(parameterTrait + " should give back the value it was instantiated with") {
      for {
        myValue <- validValues
        parameter = parameterTrait.getInstance(myValue)        
      } yield assert(parameter.value() == myValue, "Should give back value " + myValue)
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
      "tradedCurrencies" -> CurrencyPairParameter(legalCurrencies1),
      "someCoefficient" -> CoefficientParameter(legalCoefficient),
      "someOtherParameter" -> CurrencyPairParameter(legalCurrencies2)
    )
  }
  
  test("Should have a nice `toString` representation") {
    val myParameters = new StrategyParameters(
      "tradedCurrencies" -> CurrencyPairParameter(legalCurrencies1),
      "someCoefficient" -> CoefficientParameter(legalCoefficient)
    )
    val expected =
      "tradedCurrencies (type CurrencyPair) = " + legalCurrencies1 + "\n" +
      "someCoefficient (type Coefficient) = " + legalCoefficient
     
    assert(myParameters.toString().equals(expected), "\n" + myParameters + "\n Should equal:\n" + expected);
  }

  test("Should fail at instantiation with illegal parameters") {
	  val attempt = Try(new StrategyParameters("someCoefficient" -> CoefficientParameter(illegalCoefficient)))
    assert(attempt.isFailure, "Should fail to instantiate a coefficient with value " + illegalCoefficient)
  }
  
  test("Should hold the parameters and yield back their values") {
    val myParameters = new StrategyParameters(
      "tradedCurrencies" -> CurrencyPairParameter(legalCurrencies1),
      "someCoefficient" -> CoefficientParameter(legalCoefficient)
    )
    
    assert(myParameters.get[CurrencyPair]("tradedCurrencies") == Some(legalCurrencies1))
    assert(myParameters.get[Double]("someCoefficient") == Some(legalCoefficient))
  }
  
  test("Should not yield a value if the key asked was never set") {
    val myParameters = new StrategyParameters("someCoefficient" -> CoefficientParameter(legalCoefficient))
    
    val got = myParameters.get[CoefficientParameter]("unknownKey")
    assert(got == None, "Should yield nothing for an unknown key")
  }
  
  test("Should not yield a value if it doesn't have the expected type") {
	  val myParameters = new StrategyParameters("tradedCurrencies" -> CoefficientParameter(legalCoefficient))
	  
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
      CurrencyPairParameter,
      List(legalCurrencies1, legalCurrencies2),
      List(illlegalCurrencies)
    )
}