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
  
    test(parameterTrait + "should accept all values within the acceptable range") {
      for {
        value <- parameterTrait.validValues
        attempt = Try(parameterTrait.getInstance(value))
      } yield assert(attempt.isSuccess, "Should accept " + value)
    }
  }
  
  
  test("Should allow to add several parameters at a time") {
    val myParameters = new StrategyParameters(
      "tradedCurrencies" -> CurrencyPairParameter((Currency.EUR, Currency.CHF)),
      "someOtherParameter" -> CurrencyPairParameter((Currency.USD, Currency.CAD))
    )
  }
  
//  test("Should hold the parameters and yield back their values") {
//	  assert(false, "Test not implemented")
//  }
  
  testConcreteParameter(
      CoefficientParameter,
      List(0.058924379237491379, 0.0, 1.0),
      List(-1.0, -0.0001, 1.000001)
    )
    
  testConcreteParameter(
  		CurrencyPairParameter,
  		List((Currency.EUR, Currency.CHF), (Currency.GBP, Currency.USD)),
  		List((Currency.EUR, Currency.EUR), (Currency.CHF, Currency.CHF))
  		)
}